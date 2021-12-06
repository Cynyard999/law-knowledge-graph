import numpy as np
import jieba.posseg as pseg
import jieba
import joblib
from py2neo import Graph, NodeMatcher, RelationshipMatcher
from sklearn.svm import LinearSVC
from keras.models import Sequential, load_model
from keras.layers import Conv1D, GlobalAveragePooling1D, MaxPooling1D, Dense, Dropout
import json
from flask import Flask, request
import os

# graph = Graph("bolt://localhost:7687", user="neo4j", password='siegelion')
graph = Graph("http://localhost:7474", username="neo4j", password="password")
matcher = NodeMatcher(graph)
relationshipMatcher = RelationshipMatcher(graph)

# embedding_path = os.environ['HOME'] + "/word_vec"
embedding_path = "embedding/word_vec"
embedding_dict = {}


def load_embedding():
    global embedding_dict
    count = 0
    for line in open(embedding_path):
        line = line.strip().split(' ')
        if len(line) < 300:
            continue
        wd = line[0]
        vector = np.array([float(i) for i in line[1:]])
        embedding_dict[wd] = vector
        count += 1
        if count % 10000 == 0:
            print(count, 'loaded')
    print('loaded %s word embedding, finished' % count)


def get_crime_fields():
    """
    获取领域类型
    :return: 领域+标签的字典
    """
    fields = matcher.match("领域")
    label_dict = {}
    label = 0
    for f in fields:
        label_dict[label] = f["名称"]
        label += 1
    print("get crime fields: ", len(label_dict))
    return label_dict


def get_qc_train_data():
    """
    问题划分领域
    获取问题和领域对应的训练数据
    :return:
    """
    train_data = []
    # 从数据库获取
    questions = matcher.match("问题")
    for q in questions:
        qid = q.identity
        print(qid)
        match_str = "MATCH (s) - [r] -> (e) WHERE id(s) = " + str(qid) + " and type(r) = '分类为' RETURN e.`名称` as name"
        end = graph.run(match_str).data()
        name = end[0]["name"]
        train_data.append([q["描述"], name])
    print("get train data")
    return train_data


def record_crimes(crimes):
    with open("data/crime.txt", "w+") as f:
        for crime in crimes:
            f.write(crime + "\n")


def get_crimes():
    with open("data/crime.txt", "r") as f:
        return f.readlines()


def get_cp_train_data():
    """
    罪名预测
    :return:
    """
    train_data = []
    crimes = []
    cases = matcher.match("案件")
    for case in cases:
        cid = case.identity
        print(cid)
        match_str = "MATCH (s) - [r] -> (e) WHERE id(s) = " + str(cid) + " and type(r) = '判为' RETURN e.`名称` as name"
        end = graph.run(match_str).data()
        # 跳过无效数据
        if len(end) == 0:
            continue
        name = end[0]["name"]
        if name not in crimes:
            crimes.append(name)
        train_data.append([case["描述"], name])
    # 更新罪行列表
    record_crimes(crimes)
    print("get train data: ", len(train_data))
    return train_data, crimes


def train_jieba():
    jieba.load_userdict("./dict/small_crime.txt")
    jieba.load_userdict("./dict/big_crime.txt")
    jieba.load_userdict("./dict/case.txt")
    print("词性标注成功")


class SimpleQARobot:

    def __init__(self):
        train_jieba()
        self.vocab_path = './model/vocabulary.json'
        self.model_path = './model/clf.model'
        self.question_classification_path = './model/question_classification.json'
        self.vocab = self.load_vocab()
        self.question_class = self.load_question_classification()

    def load_vocab(self):
        with open(self.vocab_path, "r") as f:
            vocab = json.loads(f.read())
        return vocab

    def load_question_classification(self):
        with open(self.question_classification_path, "r") as f:
            question_classification = json.loads(f.read())
        return question_classification

    def abstract_question(self, question):
        self.abstractMap = {}
        jieba.load_userdict("./dict/small_crime.txt")
        jieba.load_userdict("./dict/big_crime.txt")
        jieba.load_userdict("./dict/case.txt")
        words = pseg.cut(question)
        abstractQuery = ''
        for word, flag in words:
            if flag == 'cg':
                abstractQuery += "cg "
                self.abstractMap['cg'] = word
            elif flag == 'cs':
                abstractQuery += 'cs '
                self.abstractMap['cs'] = word
            elif flag == 'nm':
                abstractQuery += "nm "
                self.abstractMap['nm'] = word
            else:
                abstractQuery += word + " "
        return abstractQuery

    def query_classify(self, sentence):
        tmp = np.zeros(len(self.vocab))
        list_sentence = sentence.split(' ')
        for word in list_sentence:
            if word in self.vocab:
                tmp[int(self.vocab[word])] = 1
        clf = joblib.load(self.model_path)
        index = clf.predict(np.expand_dims(tmp, 0))[0]
        return int(index), self.question_class[index]

    def query_extention(self, temp):
        params = []
        # print(self.abstractMap)
        for abs_key in self.abstractMap:
            if abs_key in temp:
                params.append(self.abstractMap[abs_key])
        return params

    def analyze_question(self, question):
        print('原始句子：{}'.format(question))
        abstr = self.abstract_question(question)
        if self.abstractMap == {}:
            return -1, ""
        print('句子抽象化结果：{}'.format(abstr))
        index, strpatt = self.query_classify(abstr)
        print('句子对应的索引{} 对应问题模板为：{}'.format(index, strpatt))
        finalpatt = self.query_extention(strpatt)
        return index, finalpatt

    def get_answer(self, index, params):
        query = ''
        result = ''
        if index == 0:
            query = "MATCH(n:罪名)-[:属于]->(m:罪类) WHERE m.名称='{}' RETURN n.名称".format(
                params[0])
            answer_cursor = graph.run(query)
            count = 0
            answer_list = []
            for item in answer_cursor:
                answer_list.append(item[0])
                count += 1
            result = '，'.join(answer_list)
            prefix_result = "有{}个，分别是".format(count)
            result = prefix_result + result + "。"
        elif index == 1:
            query = "MATCH(n:罪名)-[:属于]->(m:罪类) WHERE n.名称='{}' RETURN m.名称".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}属于的罪类是".format(params[0])
            result += answer_cursor.next()[0]
        elif index == 2:
            query = "MATCH(n:罪名) WHERE n.名称='{}' RETURN n.处罚".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}对应的处罚是".format(params[0])
            result += answer_cursor.next()[0]
        elif index == 3:
            query = "MATCH(n:罪名) WHERE n.名称='{}' RETURN n.概念".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}的概念是".format(params[0])
            result += answer_cursor.next()[0]
        elif index == 4:
            query = "MATCH(n:罪名) WHERE n.名称='{}' RETURN n.特征".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}的特征是".format(params[0])
            result += answer_cursor.next()[0]
        elif index == 5:
            query = "MATCH(n:罪名) WHERE n.名称='{}' RETURN n.链接".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}的更多信息在：".format(params[0])
            result += answer_cursor.next()[0]
            return result
        elif index == 6:
            query = "MATCH(n:案件) WHERE n.名称='{}' RETURN n.描述".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}的具体情况是".format(params[0])
            result += answer_cursor.next()[0]
        elif index == 7:
            query = "MATCH(n:案件)-[:判为]->(m:罪名) WHERE n.名称='{}' RETURN m.名称".format(
                params[0])
            answer_cursor = graph.run(query)
            result = "{}的罪名是".format(params[0])
            result += answer_cursor.next()[0]
        elif index == 8:
            query = "MATCH(n:案件)-[:判为]->(m:罪名) WHERE m.名称='{}' RETURN n.名称".format(
                params[0])
            answer_cursor = graph.run(query)
            count = 0
            answer_list = []
            for item in answer_cursor:
                answer_list.append(item[0])
                count += 1
            result = "与{}相关的案件有".format(params[0])
            result += "，".join(answer_list)
            result += "。为获取更多信息，可以输入案件名查看"
        else:
            query = "Match (n:罪名) return count(*)"
            result = "问题识别失败"
        return result

    def predict(self, sent):
        index, params = self.analyze_question(sent)
        if index == -1:
            return "无法识别，请输入准确的名称及问题"
        answer = self.get_answer(index, params)
        return answer


class CrimePredictRobot:
    def __init__(self):
        self.model_path = "model/crime_predict.model"
        self.embedding_size = 300
        self.crimes = get_crimes()
        # 训练模型时使用
        # self.train_data, self.crimes = get_cp_train_data()
        self.label_dict = self.build_crime_dict()
        self.id_dict = {j: i for i, j in self.label_dict.items()}

    def build_crime_dict(self):
        """
        构建罪名词类型
        :return:
        """
        label_dict = {}
        i = 0
        for crime in self.crimes:
            label_dict[crime] = i
            i += 1
        return label_dict

    def train_classify(self):
        """
        使用SVM进行分类
        :return:
        """
        x_train, y_train = self.load_train_data()
        model = LinearSVC()
        print("init model")
        model.fit(x_train, y_train)
        print("done")
        joblib.dump(model, self.model_path)
        y_predict = model.predict(x_train)
        all = len(y_predict)
        right = 0
        for i in range(len(y_train)):
            y = y_train[i]
            y_pred = y_predict[i]
            if y_pred == y:
                right += 1
        print('precision:%s/%s=%s' % (right, all, right / all))

    def load_train_data(self):
        """
        加载数据集
        :return:
        """
        train_x = []
        train_y = []
        count = 0
        for line in self.train_data:
            sent = line[0]
            sent_vector = self.rep_sentence_vector(sent)
            label_id = self.label_dict.get(line[1])
            count += 1
            train_x.append(sent_vector)
            train_y.append(label_id)
            if count % 10000 == 0:
                print('loaded %s lines' % count)
        return np.array(train_x), np.array(train_y)

    def seg_sent(self, s):
        wds = [i.word for i in pseg.cut(s) if i.flag[0] not in ['x', 'u', 'c', 'p', 'm', 't']]
        return wds

    def rep_sentence_vector(self, sentence):
        """
        基于wordvector，通过lookup table的方式找到句子的wordvector的表示
        :param sentence:
        :return:
        """
        word_list = self.seg_sent(sentence)
        embedding = np.zeros(self.embedding_size)
        sent_len = 0
        for index, wd in enumerate(word_list):
            if wd in embedding_dict:
                embedding += embedding_dict.get(wd)
                sent_len += 1
            else:
                continue
        return embedding / sent_len

    def predict(self, sent):
        """
        使用svm模型进行预测
        :param sent:
        :return:
        """
        model = joblib.load(self.model_path)
        represent_sent = self.rep_sentence_vector(sent)
        text_vector = np.array(represent_sent).reshape(1, -1)
        res = model.predict(text_vector)[0]
        label = self.id_dict.get(res)
        return label


class QuestionClassifyRobot:
    def __init__(self):
        self.label_dict = get_crime_fields()
        self.max_length = 60
        self.embedding_size = 300
        self.cnn_model_path = 'model/cnn_question_classify.h5'
        return

    def train_cnn(self):
        """
        cnn 模型训练
        :return:
        """
        x_train, y_train, x_test, y_test = self.split_train_set()
        model = self.build_cnn_model()
        model.fit(x_train, y_train, batch_size=100, epochs=20, validation_data=(x_test, y_test))
        model.save(self.cnn_model_path)

    def build_cnn_model(self):
        """
        构造CNN网络模型
        :return:
        """
        model = Sequential()
        model.add(Conv1D(64, 3, activation='relu', input_shape=(self.max_length, self.embedding_size)))
        model.add(Conv1D(64, 3, activation='relu'))
        model.add(MaxPooling1D(3))
        model.add(Conv1D(128, 3, activation='relu'))
        model.add(Conv1D(128, 3, activation='relu'))
        model.add(GlobalAveragePooling1D())
        model.add(Dropout(0.5))
        model.add(Dense(13, activation='softmax'))
        model.compile(loss='categorical_crossentropy',
                      optimizer='rmsprop',
                      metrics=['accuracy'])
        model.summary()
        return model

    def split_train_set(self):
        """
        划分数据集
        :return:
        """
        x, y = self.load_train_data()
        split_rate = 0.8
        index = int(len(x) * split_rate)
        x_train = x[:index]
        y_train = y[:index]
        x_test = x[index:]
        y_test = y[index:]
        return x_train, y_train, x_test, y_test

    def load_train_data(self):
        """
        加载数据集
        :return:
        """
        train_x = []
        train_y = []
        count = 0
        # 从图数据库中加载训练数据
        train_data = get_qc_train_data()
        for data in train_data:
            sent = data[0]
            # new_d = {v: k for k, v in self.label_dict.items()}
            # label = new_d[data[1]]
            try:
                re_label_dict = {v: k for k, v in self.label_dict.items()}
                label = re_label_dict[data[1]]
            except KeyError:
                continue
            # 分别获取问题和分类领域的向量
            sent_vector = self.rep_sentence_vector(sent)
            label_vector = self.label_one_hot(label)
            train_x.append(sent_vector)
            train_y.append(label_vector)
            count += 1
            if count % 10000 == 0:
                print('loaded %s lines' % count)
        return np.array(train_x), np.array(train_y)

    def seg_sent(self, s):
        wds = [i.word for i in pseg.cut(s) if i.flag[0] not in ['w', 'x']]
        return wds

    def rep_sentence_vector(self, sentence):
        """
        基于wordvector，通过lookup table的方式找到句子的wordvector的表示
        :param sentence:
        :return:
        """
        word_list = self.seg_sent(sentence)[:self.max_length]
        embedding_matrix = np.zeros((self.max_length, self.embedding_size))
        for index, wd in enumerate(word_list):
            if wd in embedding_dict:
                embedding_matrix[index] = embedding_dict.get(wd)
            else:
                continue
        len_sent = len(word_list)
        embedding_matrix = self.modify_sentence_vector(embedding_matrix, len_sent)

        return embedding_matrix

    def modify_sentence_vector(self, embedding_matrix, len_sent):
        """
        对于OOV词,通过左右词的词向量作平均,作为词向量表示
        :param embedding_matrix:
        :param len_sent:
        :return:
        """
        context_window = 2
        for indx, vec in enumerate(embedding_matrix):
            left = indx - context_window
            right = indx + context_window
            if left < 0:
                left = 0
            if right > len(embedding_matrix) - 1:
                right = -2
            context = embedding_matrix[left:right + 1]
            if vec.tolist() == [0] * 300 and indx < len_sent:
                context_vector = context.mean(axis=0)
                embedding_matrix[indx] = context_vector

        return embedding_matrix

    def label_one_hot(self, label):
        """
        对数据进行onehot映射操作
        :param label:
        :return:
        """
        one_hot = [0] * len(self.label_dict)
        one_hot[int(label)] = 1
        return one_hot

    def predict(self, sent):
        model = load_model(self.cnn_model_path)
        sentence_vector = np.array([self.rep_sentence_vector(sent)])
        res = model.predict(sentence_vector)[0].tolist()
        prob = max(res)
        label = self.label_dict.get(res.index(prob))
        return label, prob


def train_model():
    # load_embedding()
    # crime_predict_robot = CrimePredictRobot()
    # question_classify_robot = QuestionClassifyRobot()
    crime_predict_robot.train_classify()
    question_classify_robot.train_cnn()
    while 1:
        flag = input("type: ")
        if flag == "1":
            sent = input("sent: ")
            res = crime_predict_robot.predict(sent)
            print(res)
        else:
            sent = input("sent: ")
            res, prob = question_classify_robot.predict(sent)
            print(res, prob)


load_embedding()
crime_predict_robot = CrimePredictRobot()
question_classify_robot = QuestionClassifyRobot()
simple_qa_robot = SimpleQARobot()
app = Flask(__name__)


@app.route("/robot", methods=["post"])
def get_answer():
    flag = request.form["flag"]
    sent = request.form["sent"]
    if flag == "1":
        res = crime_predict_robot.predict(sent)
    elif flag == "2":
        res, prob = question_classify_robot.predict(sent)
    else:
        res = simple_qa_robot.predict(sent)
    return res


if __name__ == "__main__":
    # app.run(debug=True, use_reloader=False, port=8082)
    app.run(debug=True, use_reloader=False, port=8082)


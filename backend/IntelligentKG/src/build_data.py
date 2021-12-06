import jieba
from gensim.models import Word2Vec
import os
import re
import json
import joblib
import numpy as np
from sklearn.naive_bayes import GaussianNB
from py2neo import Graph, NodeMatcher, RelationshipMatcher


# graph = Graph("bolt://localhost:7687", user="neo4j", password='siegelion')
graph = Graph("http://localhost:7474", username="neo4j", password="password")
matcher = NodeMatcher(graph)
relationshipMatcher = RelationshipMatcher(graph)


def get_stopwords():
    # 获取停用词
    with open("data/stopwords.dat", "r") as f:
        stopwords = []
        line = f.readline()
        while line:
            # 去除停用词末尾的换行符
            stopwords.append(line.replace("\n", ""))
            line = f.readline()
    return stopwords


def train_wordvec():
    # 获取案件描述，构建语料库
    cases = matcher.match("案件")
    facts = []
    for case in cases:
        facts.append(case["描述"])
    print("success to get facts")
    stopwords = get_stopwords()
    count = 0
    sentences = []
    for fact in facts:
        words = [word for word in jieba.cut(fact) if word not in stopwords]
        line = " ".join(words)
        sentences.append(line)
        count += 1
        if count % 10000 == 0:
            print("load: " + str(count))
    print("succeed to split sentences")
    print("start to train wordvec model")
    # vector_size：词向量的维度
    # window：上下文环境的窗口大小
    # min_count：忽略出现次数低于min_count的词
    model = Word2Vec(sentences, vector_size=300, min_count=3)
    model.wv.save_word2vec_format("./model/word_vec", binary=False)


def generate_question_classification():
    question_classification_path = "./model/question_classification.json"

    def save_vocab():
        # question文件夹的问题顺序相对应
        dic = {'0': 'cg 罪名',
               '1': 'nm 罪类',
               '2': 'nm 处罚',
               '3': 'nm 概念',
               '4': 'nm 特征',
               '5': 'nm 链接',
               '6': 'cs 信息',
               '7': 'cs 罪名',
               '8': 'nm 案件'
               }
        with open(question_classification_path, 'w') as f:
            json.dump(dic, f, ensure_ascii=False)  # 会在目录下生成一个*.json的文件，文件内容是dict数据转成的json数据  ensure_ascii=False
        print("问题分类提取完成")

    if not os.path.isfile(question_classification_path):
        save_vocab()


class VocabGenerator():
    def __init__(self):
        self.question_template_path = "./question/"
        self.save_vocab_path = "./model/vocabulary.json"
        if not os.path.isfile(self.save_vocab_path):
            self.save_vocab()

    def cut_word(self, file_path):
        result_list = []
        with open(file_path, "r") as temp_f:
            for sentence in temp_f.readlines():
                sentence = sentence.strip()
                temp = jieba.lcut(sentence)
                result_list += temp
        return result_list

    def get_all_word(self):
        all_word_list = []
        for path in os.listdir(self.question_template_path):
            file_path = os.path.join(self.question_template_path, path)
            result_word_list = self.cut_word(file_path)
            all_word_list += result_word_list
        all_word_set = set(all_word_list)
        result_dict = {}
        for index, cont in enumerate(all_word_set):
            result_dict[cont] = index
        return result_dict

    def save_vocab(self):
        dic = self.get_all_word()
        with open(self.save_vocab_path, 'w') as f:
            json.dump(dic, f, ensure_ascii=False)
        print("问题词汇提取完成")


class VocabTrainer(VocabGenerator):
    def __init__(self):
        super().__init__()
        self.vocab = self.load_vocab()

    def load_vocab(self):
        with open(self.save_vocab_path, "r") as f:
            vocab = json.loads(f.read())
        return vocab

    def load_data(self):
        X = []
        Y = []
        list_file = os.listdir(self.question_template_path)
        for file_name in list_file:
            file_path = os.path.join(self.question_template_path, file_name)
            result = re.match('[[0-9]*]', file_name).span()
            start = result[0]
            end = result[1]
            with open(file_path, 'r', encoding='utf-8')as fread:
                for line in fread:
                    tmp = np.zeros(len(self.vocab))
                    Y.append(file_name[start + 1:end - 1])
                    list_sentence = jieba.lcut(line.rstrip())
                    for word in list_sentence:
                        if word in self.vocab:
                            tmp[int(self.vocab[word])] = 1
                    X.append(tmp)
        return X, Y

    def train(self):
        X, Y = self.load_data()
        clf = GaussianNB().fit(X, Y)
        joblib.dump(clf, './model/clf.model')


if __name__ == "__main__":
    train_wordvec()
    generate_question_classification()
    t = VocabTrainer()
    t.train()

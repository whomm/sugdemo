# sugdemo
 A demo for chinese search suggest / 中文搜索SUG的demo
 
* demo提供了以下4种中文sug的示例：
    1. 中文拼音首字母
    2. 拼音前缀
    3. 汉字前缀
    4. 汉字后缀
* demo中使用lucene的fst实现，该方式仅供学习。目前效果和性能表现不俗（测试过稍微大一点的数据集），生产环境用还需要结合场景细致修改一下。
这里实现的仅仅根据匹配的文字长度优先，长度相同的按照utf8的字典序排序：作为匹配的输出的排序方法。
## 说明
* 词库前缀和后缀是提前构建好的，分别创建了一个fst。  比如 word.txt 是中文词库， linux下rev命令直接将 word.txt 词翻转作为后缀的词库。
    1. rev word.txt > word_rev.txt
* 拼音首字母和拼音前缀是分别构建的fst。当然这么构建是有原因的。
* demo中使用的中文转拼音的模块是pinyin4j(https://github.com/belerweb/pinyin4j)这个拼音库中文字符有缺失而且实现性能不是太高，但这个和demo的主要内容无关所以这个地方请忽略。
* demo使用的词库的数据来源：https://github.com/fighting41love/funNLP

## 试一下
```
mvn clean package
java -jar ./target/sugdemo-1.0-SNAPSHOT.jar
```


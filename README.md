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
* demo中使用的中文转拼音的模块是pinyin4j( https://github.com/belerweb/pinyin4j )这个拼音库中文字符有缺失而且实现性能不是太高，但这个和demo的主要内容无关所以这个地方请忽略。
* demo使用的词库的数据来源：https://github.com/fighting41love/funNLP

## 试一下
```
mvn clean package
java -jar ./target/sugdemo-1.0-SNAPSHOT.jar


#输出
StopWatch '': running time (millis) = 204
-----------------------------------------
ms     %     Task name
-----------------------------------------
00052  025%  读取去重排序
00152  075%  构建fst

StopWatch '': running time (millis) = 88
-----------------------------------------
ms     %     Task name
-----------------------------------------
00045  051%  读取去重排序
00043  049%  构建fst

中文前后缀sug---------------
input to sreach[725b 8089]
 target=52115 label=0x8089 output=1636 arcArray(idx=24 of 35)
 StopWatch '': running time (millis) = 9
 -----------------------------------------
 ms     %     Task name
 -----------------------------------------
 00009  100%  开始sug

 牛肉丸
 牛肉干
 牛肉片
 牛肉羹
 牛肉面
 牛肉饼
 牛肉夹馍
 牛肉小包
 牛肉泡馍
 牛肉炒粉
 input to sreach[8089 725b]
  target=55678 label=0x725b output=1643 arcArray(idx=58 of 108)
  StopWatch '': running time (millis) = 0
  -----------------------------------------
  ms     %     Task name
  -----------------------------------------
  00000  �  开始sug

  卤牛肉
  炸牛肉
  牦牛肉
  姜丝牛肉
  芥兰牛肉
  百合牛肉
  川味牛肉
  美味牛肉
  咖哩牛肉
  咖喱牛肉
  拼音首字母sug---------------
  StopWatch '': running time (millis) = 430
  -----------------------------------------
  ms     %     Task name
  -----------------------------------------
  00407  095%  读取
  00023  005%  构建fst

  input to sreach[6e 72]
   target=27391 label=0x72 final output=8 arcArray(idx=14 of 21)
   StopWatch '': running time (millis) = 1
   -----------------------------------------
   ms     %     Task name
   -----------------------------------------
   00001  100%  开始sug

   牛乳;纳仁;
   牛肉饼;
   嫩肉粉;
   牛肉干;牛肉羹;
   牛肉面;
   牛肉片;
   南乳肉;
   牛肉丸;
   牛肉炒粉;
   南肉春笋;
   牛肉焦饼;
   牛肉夹馍;
   牛肉泡馍;
   牛肉荞面;
   牛肉烧卖;牛肉烧麦;
   牛肉馅饼;牛肉小包;
   南乳小排;
   牛肉蛋花粥;
   南乳小凤饼;
   牛肉末炒芹菜;
   拼音前缀sug---------------
   StopWatch '': running time (millis) = 235
   -----------------------------------------
   ms     %     Task name
   -----------------------------------------
   00184  078%  读取
   00051  022%  构建fst

   input to sreach[6e 69 75 72]
    target=60982 label=0x72 output=2 arcArray(idx=10 of 17)
	StopWatch '': running time (millis) = 1
	-----------------------------------------
	ms     %     Task name
	-----------------------------------------
	00001  100%  开始sug

	牛乳;
	牛肉干;
	牛肉丸;
	牛肉饼;
	牛肉羹;
	牛肉面;
	牛肉片;
	牛肉夹馍;
	牛肉泡馍;
	牛肉炒粉;
	牛肉烧卖;牛肉烧麦;
	牛肉小包;
	牛肉焦饼;
	牛肉荞面;
	牛肉馅饼;
	牛肉蛋花粥;
	牛肉末炒芹菜;
	牛肉丝炒芹菜;
	牛肉蔬菜浓汤;
```


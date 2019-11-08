import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.springframework.util.StopWatch;

public class sugdemo {

    /**
     * 字典排序排序后用来构建fst
     */
    public  static  class DictSort implements Comparator<IntsRef> {
        public int compare(IntsRef o1, IntsRef o2) {
            return o1.compareTo(o2);
        }
    }


    /**
     * 长度相同的用字典排序，这个排序是为了构建fst的ouput，利用output的大小，优先将词的长度相同的给sug出来
     */
    public  static class LenDictSort implements Comparator<IntsRef> {
        public int compare(IntsRef o1, IntsRef o2) {
            if (o1.length != o2.length) {
                return o1.length - o2.length;
            }
            return o1.compareTo(o2);
        }
    }


    /**
     * 汉字的拼音和拼音首字母 (忽略音调)
     */
    public  static  class PinyinDecs {
        public String Pinyin;
        public String PinyinPrifix;
    }


    /**
     * 做拼音sug时候用的fst和词典
     */
    public  static  class PySug {
        public FST<Long> fst;
        public HashMap<IntsRef,ArrayList<String>> wordMap;

    }


    /**
     * 获取拼音
     * @param src
     * @return
     */
    public static PinyinDecs getPinYin(String src){
        src = src.toLowerCase();
        char[] hz = null;
        hz = src.toCharArray();//该方法的作用是返回一个字符数组，该字符数组中存放了当前字符串中的所有字符
        String[] py = new String[hz.length];//该数组用来存储
        //设置汉子拼音输出的格式
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        String pys = ""; //存放拼音字符串
        String pypr = "";
        int len = hz.length;

        try {
            for (int i = 0; i < len ; i++ ){
                //先判断是否为汉字字符
                if(Character.toString(hz[i]).matches("[\\u4E00-\\u9FA5]+")){
                    //将汉字的几种全拼都存到py数组中
                    py = PinyinHelper.toHanyuPinyinStringArray(hz[i],format);
                    //取出改汉字全拼的第一种读音，并存放到字符串pys后

                    if (py == null) {
                        // System.out.println(hz[i]+" pinyin 找不到");
                        // 【很多找不到的 先忽略吧 重点不在这】
                    }
                    if (py != null) {
                        pys += py[0];
                        pypr += py[0].charAt(0);
                    } else {
                        pys += Character.toString(hz[i]);
                        pypr += hz[i];
                    }


                }else{
                    //如果不是汉字字符，间接取出字符并连接到 pys 后
                    pys += Character.toString(hz[i]);
                    pypr += hz[i];
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e){
            e.printStackTrace();
        }

        PinyinDecs pinfo = new PinyinDecs();
        pinfo.Pinyin = pys;
        pinfo.PinyinPrifix = pypr;

        return pinfo;
    }


    /**
     * 字符串转int[]
     * @param xarrs
     * @return
     */
    public static int[] stringToInt(String xarrs) {
        char[] arrs = xarrs.toCharArray();
        int[] ints = new int[arrs.length];
        for (int i = 0; i < arrs.length; i++) {
            ints[i] = (int) arrs[i];
        }
        return ints;
    }


    public static void sug(String forsug, FST<Long> fst, boolean revert) throws Exception {

        StopWatch clock = new StopWatch();

        IntsRef input = new IntsRef(stringToInt(forsug), 0, forsug.length());
        System.out.println("input to sreach" + input.toString());
        FST.Arc<Long> arc = fst.getFirstArc(new FST.Arc<Long>());
        FST.BytesReader fstReader = fst.getBytesReader();
        for (int i = 0; i < input.length; i++) {
            if (fst.findTargetArc(input.ints[input.offset + i], arc, arc, fstReader) == null) {
                System.out.println("没找到。。。");
                return;
            }
        }
        System.out.println(arc.toString());
        clock.start("开始sug");
        // 从 "位置开始找走到终止状态最近的2条路径
        Util.TopResults<Long> results = Util.shortestPaths(fst, arc, PositiveIntOutputs.getSingleton().getNoOutput(), new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                return o1.compareTo(o2);
            }
        }, 10, true);
        clock.stop();
        System.out.println(clock.prettyPrint());

        for (Util.Result<Long> result : results) {
            IntsRef intsRef = result.input;
            StringBuilder xout = new StringBuilder();

            xout.append(forsug);
            for (int i = 0; i < result.input.length; i++) {
                xout.append((char) result.input.ints[i]);
            }
            if (revert) {
                System.out.println(xout.reverse());
            } else {
                System.out.println(xout.toString());
            }
        }


    }

    public static FST<Long> buildHzFST(String wordFile) throws Exception {

        StopWatch clock = new StopWatch();

        clock.start("读取去重排序");

        //读取文件并去重
        HashMap<IntsRef, Long> outMap = new HashMap<IntsRef, Long>();
        FileInputStream fis = new FileInputStream(wordFile);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader br = new BufferedReader(isr);


        String line = "";
        while ((line = br.readLine()) != null) {

            String tkey = line.trim();
            if (tkey.length() <= 0) {
                continue;
            }
            IntsRef xint = new IntsRef(stringToInt(tkey), 0, tkey.length());
            outMap.put(xint, 0L);

        }
        br.close();
        isr.close();
        fis.close();



        ArrayList<IntsRef> wordArray = new ArrayList<IntsRef>();
        for (IntsRef key : outMap.keySet()) {
            wordArray.add(key);
        }


        //这个排序是为了编辑输出的顺序（起到一个权重的作用 可用来做粗排序） 字符串长度比较相同的再按照字典序排序
        wordArray.sort(new LenDictSort());
        for (int i = 0; i < wordArray.size(); i++) {
            outMap.put(wordArray.get(i), (long) (i + 1));

        }

        //排序这个是为了构建fst 构建fst按照intsrfs的字典序
        wordArray.sort(new DictSort());
        clock.stop();


        clock.start("构建fst");
        //开始创建fst
        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        Builder<Long> builder = new Builder<Long>(FST.INPUT_TYPE.BYTE4, outputs);
        for (int i = 0; i < wordArray.size(); i++) {
            builder.add(wordArray.get(i), outMap.get(wordArray.get(i)));
        }
        FST<Long> fst = builder.finish();
        clock.stop();


        System.out.println(clock.prettyPrint());

        return fst;
    }






    public static void pinyinsug(String forsug, FST<Long> fst, HashMap<IntsRef, ArrayList<String>> wordMap) throws Exception {

        StopWatch clock = new StopWatch();

        IntsRef input = new IntsRef(stringToInt(forsug), 0, forsug.length());
        System.out.println("input to sreach" + input.toString());
        FST.Arc<Long> arc = fst.getFirstArc(new FST.Arc<Long>());
        FST.BytesReader fstReader = fst.getBytesReader();
        for (int i = 0; i < input.length; i++) {
            if (fst.findTargetArc(input.ints[input.offset + i], arc, arc, fstReader) == null) {
                System.out.println("没找到。。。");
                return;
            }
        }
        System.out.println(arc.toString());
        clock.start("开始sug");
        // 从 "位置开始找走到终止状态最近的2条路径
        Util.TopResults<Long> results = Util.shortestPaths(fst, arc, PositiveIntOutputs.getSingleton().getNoOutput(), new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                return o1.compareTo(o2);
            }
        }, 20, true);
        clock.stop();
        System.out.println(clock.prettyPrint());

        for (Util.Result<Long> result : results) {
            IntsRef intsRef = result.input;

            StringBuilder xout = new StringBuilder();

            xout.append(forsug);
            for (int i = 0; i < result.input.length; i++) {
                xout.append((char) result.input.ints[i]);
            }
            //System.out.println("py:"+xout);

            IntsRef gg = new IntsRef(stringToInt(xout.toString()),0,xout.toString().length());
            if (wordMap.containsKey(gg)) {
                ArrayList<String> tmp = wordMap.get(gg);
                for (int i =0 ; i<tmp.size(); i++){
                    System.out.print(tmp.get(i)+";");
                }
                System.out.println();
            }


        }


    }

    public static PySug buildPinyinFST(String wordFile,boolean pyPrefix) throws Exception {

        StopWatch clock = new StopWatch();

        clock.start("读取");

        //拼音对应的词 同一个读音可能对应多个词
        //读取文件构建拼音到词组的映射
        HashMap<IntsRef,ArrayList<String>> wordMap = new HashMap<IntsRef, ArrayList<String>>();

        FileInputStream fis = new FileInputStream(wordFile);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line = "";
        while ((line = br.readLine()) != null) {

            String tkey = line.trim();
            if (tkey.length() <= 0) {
                continue;
            }

            PinyinDecs  pinfo = getPinYin(tkey);
            String pinyinstr = "";
            if (pyPrefix){
                pinyinstr = pinfo.PinyinPrifix;
            } else {
                pinyinstr = pinfo.Pinyin;
            }
            IntsRef xint = new IntsRef(stringToInt(pinyinstr), 0, pinyinstr.length());
            if (wordMap.containsKey(xint)){
                wordMap.get(xint).add(tkey);
            } else {
                ArrayList<String> tmp = new ArrayList<String>();
                tmp.add(tkey);
                wordMap.put(xint, tmp);
            }
        }
        br.close();
        isr.close();
        fis.close();




        ArrayList<IntsRef> wordArray = new ArrayList<IntsRef>();

        for (IntsRef key : wordMap.keySet()) {
            wordArray.add(key);
        }

        //记录输出权重
        HashMap<IntsRef,Integer> orderMap = new HashMap<IntsRef,Integer>();
        //这个排序是为了编辑输出的顺序（起到一个权重的作用 可用来做粗排序） 字符串长度比较相同的再按照字典序排序
        wordArray.sort(new LenDictSort());
        for (int i = 0; i < wordArray.size(); i++) {
            orderMap.put(wordArray.get(i),  (i + 1));

        }

        //排序这个是为了构建fst 构建fst按照intsrfs的字典序
        wordArray.sort(new DictSort());

        clock.stop();


        clock.start("构建fst");
        //开始创建fst
        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        Builder<Long> builder = new Builder<Long>(FST.INPUT_TYPE.BYTE4, outputs);
        for (int i = 0; i < wordArray.size(); i++) {
            builder.add(wordArray.get(i), (long)orderMap.get(wordArray.get(i)));
        }
        FST<Long> fst = builder.finish();
        clock.stop();


        System.out.println(clock.prettyPrint());

        PySug ps = new PySug();
        ps.fst = fst;
        ps.wordMap = wordMap;

        return ps;
    }

    public static void main(String[] args) throws Exception {

        FST<Long> fst = buildHzFST("./src/main/resources/word.txt");
        FST<Long> revfst = buildHzFST("./src/main/resources/word_rev.txt");

        System.out.println("中文前后缀sug---------------");
        String forsug = "牛肉";

        if (forsug.length() > 1) {
            sug(forsug, fst, false);
            sug(new StringBuilder(forsug).reverse().toString(), revfst, true);

        } else {
            sug(forsug, fst, false);
            sug(forsug, revfst, true);
        }

        System.out.println("拼音首字母sug---------------");
        PySug pysug = buildPinyinFST("./src/main/resources/word.txt",true);
        pinyinsug("nr",pysug.fst,pysug.wordMap);


        System.out.println("拼音前缀sug---------------");
        PySug pysug2 = buildPinyinFST("./src/main/resources/word.txt",false);
        pinyinsug("niur",pysug2.fst,pysug2.wordMap);


    }
}

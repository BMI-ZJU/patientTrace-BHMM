package cn.edu.zju.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.edu.zju.util.Utils.sbc2dbc;

public class Rules {
    public static final String pattern = "&|[\\uFE30-\\uFFA0]|‘’|“”";

    // 舍弃该方法，以全角转半角符号代替该方法
//    /**
//     * 将中文的左右括号替换为英文的 (passed)
//     */
//    public static String replaceBrackets(String origin) {
//        return origin.replaceAll("\\uff08", "(").replaceAll("\\uff09", ")");
//    }

    /**
     * 去除所有的★以及开头的一些标点 (passed)
     */
    private static String removeStar(String origin) {
        return origin.replaceAll("\\u2605|^\\u3001|^\\.|^\\\\|^`", "");
    }

    /**
     * 输血
     */
    private static String replaceTransfusion(String origin) {
        if (origin.equals("B型钠尿酸肽") || origin.equals("O型血交叉配血试验")) return origin;
        String reg = "输.*型|输.*细胞|输.*血小板|输.*血浆|[ABO]B?型";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(origin);
        if (matcher.find()) {
            return "输血";
        }
        return origin;
    }

    /**
     * 去除最后一个 '等' 字
     */
    private static String removeEt(String name) {
        String reg = "等$";
        return name.replaceAll(reg, "");
    }

    /**
     * 将***胰岛素注射液 全转为 胰岛素注射液
     */
    private static String replaceInsulin(String origin) {
        String reg = "胰岛素.*注射";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(origin);
        if (matcher.find()) {
            return "胰岛素注射液";
        }
        return origin;
    }

    /**
     * 将干预行为的名称进行转换
     */
    public static String transName(String name) {
        name = sbc2dbc(name).toUpperCase(); // 全角 --> 半角，全大写
        String temp1 = removeStar(name); // 去除星星
        String temp2 = replaceInsulin(temp1);
        return temp2;
    }

    public static String transLabtest(String name) {
        name = sbc2dbc(name).toUpperCase();
        String temp1 = removeEt(name);
        return temp1;
    }

    /**
     * 射频消融术 -> 射频消融术
     */
    private static String oper1(String name) {
        String reg = "射频消融";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return "射频消融术";
        }
        return name;
    }

    /**
     * 心脏起搏器
     */
    private static String oper2(String name) {
        String reg = "起搏器[植置]入";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return "心脏起搏器植入";
        }
        return name;
    }

    /**
     * 冠脉旁路移植手术
     */
    private static String oper3(String name) {
        String reg = "冠状动脉旁路移植";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return "冠脉旁路移植手术";
        }
        return name;
    }

    /**
     * 主动脉瓣置换术
     */
    private static String oper4(String name) {
        String reg = "主动脉瓣置换";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return "主动脉瓣置换术";
        }
        return name;
    }

    /**
     * 冠状动脉造影
     */
    private static String oper5(String name) {
        String reg = "冠脉造影|冠状动脉造影";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return "冠脉造影";
        }
        return name;
    }

    private static String oper6(String name) {
        String reg1 = "三尖瓣成形";
        Pattern pattern1 = Pattern.compile(reg1);
        Matcher matcher1 = pattern1.matcher(name);
        if (matcher1.find()) {
            return "三尖瓣成形术";
        }

        String reg2 = "二尖瓣成形";
        Pattern pattern2 = Pattern.compile(reg2);
        Matcher matcher2 = pattern2.matcher(name);
        if (matcher2.find()) {
            return "二尖瓣成形术";
        }

        String reg3 = "肾动脉造影";
        Pattern pattern3 = Pattern.compile(reg3);
        Matcher matcher3 = pattern3.matcher(name);
        if (matcher3.find()) {
            return "肾动脉造影术";
        }

        String reg4 = "室间隔缺损修补";
        Pattern pattern4 = Pattern.compile(reg4);
        Matcher matcher4 = pattern4.matcher(name);
        if (matcher4.find()) {
            return "室间隔缺损修补术";
        }

        String reg5 = "心脏电生理检查";
        Pattern pattern5 = Pattern.compile(reg5);
        Matcher matcher5 = pattern5.matcher(name);
        if (matcher5.find()) {
            return "心脏电生理检查术";
        }

        String reg6 = "主动脉气囊";
        Pattern pattern6 = Pattern.compile(reg6);
        Matcher matcher6 = pattern6.matcher(name);
        if (matcher6.find()) {
            return "主动脉气囊反搏治疗";
        }

        String reg7 = "主动脉球囊";
        Pattern pattern7 = Pattern.compile(reg7);
        Matcher matcher7 = pattern7.matcher(name);
        if (matcher7.find()) {
            return "主动脉球囊治疗";
        }

        return name;
    }

    public static String[] transOper(String name) {
        name = sbc2dbc(name).toUpperCase(); // 全角 --> 半角，全大写
        String[] separate = name.split("[+\\uff0b=、并和及.]");
        for (int i=0; i<separate.length; i++) {
            separate[i] = oper1(separate[i]);
            separate[i] = oper2(separate[i]);
            separate[i] = oper3(separate[i]);
            separate[i] = oper4(separate[i]);
            separate[i] = oper5(separate[i]);
            separate[i] = oper6(separate[i]);
        }
        return separate;
    }

    public static void main(String[] args) {
        String s = "左室造影=左心导管检查";
        String[] ss = transOper(s);

        for (String i : ss ) {
            System.out.println(i);
        }
    }


}

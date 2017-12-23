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
     * 如果在医嘱中匹配到以下内容(一般都是手术信息)则全部删除，默认以记录在手术信息中的为准
     * <ul>
     *     <li>常规准备</li>
     *     <li>常规*行</li>
     *     <li>定于*行</li>
     *     <li>行</li>
     *     <li>今*行</li>
     *     <li>*局*行</li>
     *     <li>明*行</li>
     *     <li>拟*行</li>
     *     <li>全麻*行</li>
     *     <li>上*行</li>
     *     <li>体外*行</li>
     *     <li>下*行</li>
     *     <li>中*行</li>
     * </ul>
     */
    public static boolean isMatchOperation(String origin) {
        String regex = "常规准备|常规.*行|定于.*行|^行|今*行|.*局.*行|明.*行|拟.*行|全麻.*行|上.*行|体外.*行|下.*行|中.*行|^拟|局麻|全麻";
        Pattern pattern  = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(origin);
        return matcher.find();
    }
    /**
     * 脱呼吸机训练，脱机...等全转为  脱机 (passed)
     */
    public static String replaceRespirator(String origin) {
        String reg = "脱.*机";
        return origin.replaceAll(reg, "脱机");
    }

    /**
     * 匹配备*皮，全都转换成为备皮 (passed)
     */
    private static String replacePrepareSkin(String origin) {
        String reg = ".*[备背].*皮.*";
        return origin.replaceAll(reg, "备皮");
    }

    /**
     * 匹配 .*会诊， 全部转为会诊
     */
    private static String replaceConsultation(String origin) {
        String reg = "会诊";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(origin);
        if (matcher.find()) {
            return "会诊";
        }
        return origin;
    }

    /**
     * 匹配 转.*[院科区]，全部转为 转科
     */
    private static String replaceTransfer(String origin) {
        String reg0 = "^转[移化]";
        Pattern pattern0 = Pattern.compile(reg0);
        Matcher matcher0 = pattern0.matcher(origin);
        if (matcher0.find())  return origin;

        String reg1 = "^转";
        Pattern pattern1 = Pattern.compile(reg1);
        Matcher matcher1 = pattern1.matcher(origin);
        if (matcher1.find()) {
            return "转科";
        }
        return origin;
    }

    /**
     * 换药
     */
    private static String replaceDressing(String origin) {
        String reg = "换药";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(origin);
        if (matcher.find()) {
            return "换药";
        }
        return origin;
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
     * 将干预行为的名称进行转换
     */
    public static String transName(String name) {
        name = sbc2dbc(name).toUpperCase(); // 全角 --> 半角，全大写
        String temp1 = replacePrepareSkin(name); // 备*皮 --> 备皮
        String temp2 = replaceConsultation(temp1); // *会诊 --> 会诊
        String temp3 = replaceTransfer(temp2); // 转*区 --> 转科
        String temp4 = removeStar(temp3); // 去除星星
        String temp5 = replaceRespirator(temp4); // 脱机 训练
        String temp6 = replaceTransfusion(temp5); // 输**红细胞，血小板 --> 输血
        String temp7 = replaceDressing(temp6); // 换药
        return temp7;
    }


    public static void main(String[] args) {
        String temp1 = "ABO血型反定型(A.B抗原)国产";
        String temp2 = "AB型RH(D )阳性新鲜冰冻血浆";
        String temp3 = "B型钠尿酸肽";

        System.out.println(replaceTransfusion(temp1));
        System.out.println(replaceTransfusion(temp2));
        System.out.println(replaceTransfusion(temp3));
    }


}

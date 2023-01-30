package org.bluewind.authclient.util;

import org.bluewind.authclient.util.idgen.ObjectId;
import org.bluewind.authclient.util.idgen.Snowflake;

import java.util.UUID;

/**
 * token字符串生成工具类
 *
 * @author liuxingyu01
 * @version 2022-05-21 9:53
 **/
public class TokenGenUtil {

    final static String TOKEN_STYLE_UUID = "uuid";

    final static String TOKEN_STYLE_SNOWFLAKE = "snowflake";

    final static String TOKEN_STYLE_OBJECTID = "objectid";

    final static String TOKEN_STYLE_RANDOM128 = "random128";

    /**
     * 根据参数生存不同风格的token字符串
     *
     * @param tokenStyle token风格
     * @return token字符串
     */
    public static String genTokenStr(String tokenStyle) {
        String token;
        switch (tokenStyle) {
            case TOKEN_STYLE_UUID:
                token = UUID.randomUUID().toString().replace("-", "");
                break;
            case TOKEN_STYLE_SNOWFLAKE:
                token = Snowflake.nextId();
                break;
            case TOKEN_STYLE_OBJECTID:
                token = ObjectId.nextId();
                break;
            case TOKEN_STYLE_RANDOM128:
                token = CommonUtil.getRandomString(128);
                break;
            default:
                token = UUID.randomUUID().toString().replaceAll("-", "");
                break;
        }
        return token;
    }

}

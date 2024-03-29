package org.tinycloud.security.util;

import org.tinycloud.security.util.idgen.NanoId;
import org.tinycloud.security.util.idgen.ObjectId;
import org.tinycloud.security.util.idgen.Snowflake;
import org.tinycloud.security.util.idgen.ulid.UlidCreator;

import java.util.UUID;

/**
 * token字符串生成工具类
 *
 * @author liuxingyu01
 * @version 2022-05-21 9:53
 **/
public class TokenGenUtil {

    final static String TOKEN_STYLE_UUID = "uuid";

    final static String TOKEN_STYLE_ULID = "ulid";

    final static String TOKEN_STYLE_SNOWFLAKE = "snowflake";

    final static String TOKEN_STYLE_OBJECTID = "objectid";

    final static String TOKEN_STYLE_RANDOM128 = "random128";

    final static String TOKEN_STYLE_NANOID = "nanoid";

    /**
     * 根据参数生存不同风格的token字符串
     *
     * @param tokenStyle token风格
     * @return token字符串
     */
    public static String genTokenStr(String tokenStyle) {
        if (tokenStyle == null || tokenStyle.isEmpty()) {
            tokenStyle = TOKEN_STYLE_UUID;
        }
        String token;
        switch (tokenStyle) {
            case TOKEN_STYLE_UUID:
                token = UUID.randomUUID().toString().replace("-", "");
                break;
            case TOKEN_STYLE_ULID:
                token = UlidCreator.getUlid().toLowerCase();
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
            case TOKEN_STYLE_NANOID:
                token = NanoId.INSTANCE.randomNanoId();
                break;
            default:
                token = UUID.randomUUID().toString().replaceAll("-", "");
                break;
        }
        return token;
    }

}

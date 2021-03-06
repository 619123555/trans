package com.open.common.utils.signature;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/** @author admin @创建人：YYX @创建时间：2016/3/18 15:27 */
public class Base64Util {
  /**
   * BASE64解密
   *
   * @param key
   * @return
   * @throws Exception
   */
  public static byte[] decryptBASE64(String key) throws Exception {
    return (new BASE64Decoder()).decodeBuffer(key);
  }

  /**
   * BASE64 加密
   *
   * @param key
   * @return
   * @throws Exception
   */
  public static String encryptBASE64(byte[] key) throws Exception {
    return (new BASE64Encoder()).encodeBuffer(key);
  }
}

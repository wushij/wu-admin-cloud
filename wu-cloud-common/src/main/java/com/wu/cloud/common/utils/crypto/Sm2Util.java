package com.wu.cloud.common.utils.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.wu.cloud.common.exception.BusinessException;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;

/**
 * 国密 SM2 满血标准非对称密码算法工具类
 *
 * <p>标准依据：GM/T 0003-2012《SM2 椭圆曲线公钥密码算法》
 * 特性：
 * 1. 密文格式：严格遵循国家商用密码标准 C1C3C2 模式；
 * 2. 公钥格式：支持 04 开头的 65 字节（130 位十六进制）未压缩格式，或 64 位无 04 坐标格式；
 * 3. 私钥格式：32 字节（64 位十六进制）大整数标量；
 * 4. 签名验签：基于 SM3 摘要与默认 IDA（1234567812345678）的 SM2 椭圆曲线数字签名；
 * 5. 与前端/移动端 sm-crypto（cipherMode=1，即 C1C3C2）互通：
 *    前端加密的密文可直接由本类解密（sm-crypto 的 C1 不带 04 前缀，本类自动兼容）。</p>
 */
public final class Sm2Util {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final X9ECParameters SM2_PARAMS = GMNamedCurves.getByName("sm2p256v1");
    private static final ECDomainParameters DOMAIN_PARAMS = new ECDomainParameters(
            SM2_PARAMS.getCurve(), SM2_PARAMS.getG(), SM2_PARAMS.getN(), SM2_PARAMS.getH());

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm2Util() {
    }

    /**
     * SM2 密钥对载体
     */
    public record Sm2KeyPair(String publicKeyHex, String privateKeyHex) {
    }

    /**
     * 生成全新的满血标准 SM2 密钥对
     *
     * @return 包含 130 位 Hex 公钥（04 开头未压缩）与 64 位 Hex 私钥的密钥对对象
     */
    public static Sm2KeyPair generateKeyPair() {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(new ECKeyGenerationParameters(DOMAIN_PARAMS, SECURE_RANDOM));
        AsymmetricCipherKeyPair pair = generator.generateKeyPair();

        ECPublicKeyParameters pub = (ECPublicKeyParameters) pair.getPublic();
        ECPrivateKeyParameters pri = (ECPrivateKeyParameters) pair.getPrivate();

        // 导出 04 开头 65 字节未压缩公钥
        byte[] pubBytes = pub.getQ().getEncoded(false);
        String pubHex = HexUtil.encodeHexStr(pubBytes);

        // 导出 32 字节私钥
        byte[] priBytes = pri.getD().toByteArray();
        // 抹除 BigInteger 前导符号位 00
        if (priBytes.length == 33 && priBytes[0] == 0) {
            byte[] tmp = new byte[32];
            System.arraycopy(priBytes, 1, tmp, 0, 32);
            priBytes = tmp;
        } else if (priBytes.length < 32) {
            byte[] tmp = new byte[32];
            System.arraycopy(priBytes, 0, tmp, 32 - priBytes.length, priBytes.length);
            priBytes = tmp;
        }
        String priHex = HexUtil.encodeHexStr(priBytes);

        return new Sm2KeyPair(pubHex, priHex);
    }

    /**
     * 解析公钥为 ECPublicKeyParameters
     */
    public static ECPublicKeyParameters parsePublicKey(String publicKeyHex) {
        if (StrUtil.isBlank(publicKeyHex)) {
            throw new IllegalArgumentException("SM2 公钥不能为空");
        }
        String hex = publicKeyHex.trim();
        // 如果未带 04 前缀且为 128 位十六进制，自动补齐 04 未压缩标识
        if (hex.length() == 128) {
            hex = "04" + hex;
        }
        byte[] pubBytes = HexUtil.decodeHex(hex);
        ECPoint point = DOMAIN_PARAMS.getCurve().decodePoint(pubBytes);
        return new ECPublicKeyParameters(point, DOMAIN_PARAMS);
    }

    /**
     * 解析私钥为 ECPrivateKeyParameters
     */
    public static ECPrivateKeyParameters parsePrivateKey(String privateKeyHex) {
        if (StrUtil.isBlank(privateKeyHex)) {
            throw new IllegalArgumentException("SM2 私钥不能为空");
        }
        String hex = privateKeyHex.trim();
        BigInteger d = new BigInteger(1, HexUtil.decodeHex(hex));
        return new ECPrivateKeyParameters(d, DOMAIN_PARAMS);
    }

    /**
     * SM2 非对称加密（标准 C1C3C2 模式）
     *
     * @param publicKeyHex 130 位或 128 位十六进制公钥
     * @param plainText    待加密明文字符串
     * @return 包含 C1C3C2 模式密文的十六进制字符串（通常以 04 开头，表示未压缩的点 C1）
     */
    public static String encryptHex(String publicKeyHex, String plainText) throws Exception {
        if (plainText == null) {
            return null;
        }
        ECPublicKeyParameters pubKey = parsePublicKey(publicKeyHex);
        SM2Engine engine = new SM2Engine(new SM3Digest(), SM2Engine.Mode.C1C3C2);
        CipherParameters params = new ParametersWithRandom(pubKey, SECURE_RANDOM);
        engine.init(true, params);

        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes = engine.processBlock(plainBytes, 0, plainBytes.length);
        return HexUtil.encodeHexStr(cipherBytes);
    }

    /**
     * SM2 非对称解密（标准 C1C3C2 模式）
     *
     * @param privateKeyHex 64 位十六进制私钥
     * @param cipherHex     C1C3C2 模式密文（十六进制字符串，若带 04 前缀或未带均可自动适配）
     * @return 解密还原的 UTF-8 明文字符串
     */
    public static String decryptHex(String privateKeyHex, String cipherHex) throws Exception {
        if (StrUtil.isBlank(cipherHex)) {
            return cipherHex;
        }
        String hex = cipherHex.trim();
        ECPrivateKeyParameters priKey = parsePrivateKey(privateKeyHex);

        // 兼容两种 C1 表示：
        // 1) BouncyCastle encryptHex 输出：04 || x || y || C3 || C2
        // 2) sm-crypto doEncrypt 输出：x || y || C3 || C2（不带 04）
        // 不能仅凭首字节是否为 "04" 判断：无前缀密文的 x 首字节也可能恰好是 0x04。
        Exception last = null;
        for (String candidate : new String[] { hex, "04" + hex }) {
            try {
                byte[] cipherBytes = HexUtil.decodeHex(candidate);
                SM2Engine engine = new SM2Engine(new SM3Digest(), SM2Engine.Mode.C1C3C2);
                engine.init(false, priKey);
                byte[] plainBytes = engine.processBlock(cipherBytes, 0, cipherBytes.length);
                return new String(plainBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last != null ? last : new IllegalArgumentException("SM2 密文格式非法");
    }

    /**
     * SM2 数字签名（私钥签名，带 SM3 摘要）
     *
     * @param privateKeyHex 64 位十六进制私钥
     * @param dataBytes     待签名数据明文
     * @return BouncyCastle 的 DER 编码签名（十六进制）。
     * 注意：这不是前端 sm-crypto 使用的 64 字节 r||s 拼接格式；
     * 如需与 sm-crypto.doSignature 互通，须额外做 DER 解码并按 32 字节定长拼接 r、s。
     */
    public static String signHex(String privateKeyHex, byte[] dataBytes) throws Exception {
        ECPrivateKeyParameters priKey = parsePrivateKey(privateKeyHex);
        SM2Signer signer = new SM2Signer(new SM3Digest());
        signer.init(true, new ParametersWithRandom(priKey, SECURE_RANDOM));
        signer.update(dataBytes, 0, dataBytes.length);
        byte[] derSignature = signer.generateSignature();
        return HexUtil.encodeHexStr(derSignature);
    }

    /**
     * SM2 数字签名验签（公钥验签）
     *
     * @param publicKeyHex 130 位或 128 位十六进制公钥
     * @param dataBytes    待验证明文数据
     * @param signatureHex 签名十六进制串（DER 编码）
     * @return 验签通过返回 true，失败返回 false
     */
    public static boolean verifyHex(String publicKeyHex, byte[] dataBytes, String signatureHex) {
        try {
            ECPublicKeyParameters pubKey = parsePublicKey(publicKeyHex);
            SM2Signer signer = new SM2Signer(new SM3Digest());
            signer.init(false, pubKey);
            signer.update(dataBytes, 0, dataBytes.length);
            byte[] sigBytes = HexUtil.decodeHex(signatureHex.trim());
            return signer.verifySignature(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析并解密客户端提交的用户口令（密码）。
     * <p>若客户端采用国密 SM2 满血标准公钥加密（支持 sm2: 前缀或标准十六进制密文），
     * 使用系统 SM2 椭圆私钥解密为原始明文；
     * 若未加密或非 SM2 密文格式，安全原样返回，确保向后兼容与绝对零回归。</p>
     *
     * @param privateKeyHex 服务端 SM2 椭圆私钥
     * @param rawPassword   原始传入密码字符串
     * @return 解密后的明文密码
     */
    public static String decryptPasswordIfEncrypted(String privateKeyHex, String rawPassword) {
        if (StrUtil.isBlank(rawPassword) || StrUtil.isBlank(privateKeyHex)) {
            return rawPassword;
        }
        String trimmed = rawPassword.trim();
        // 1. 显式包含 sm2: 标识前缀的满血国密密文
        if (trimmed.startsWith("sm2:")) {
            String cipherHex = trimmed.substring(4);
            try {
                return decryptHex(privateKeyHex, cipherHex);
            } catch (Exception e) {
                throw new BusinessException(400, "密码解密失败，请刷新页面后重试");
            }
        }
        // 2. 无前缀但长度符合 SM2 C1C3C2 密文特征。
        //    带 04 前缀最短为 130(C1)+64(C3)+2(C2)=196；sm-crypto 无前缀最短为 128+64+2=194，
        //    这里取 192 作为保守下限，避免漏判短口令密文。
        if (trimmed.length() >= 192 && HexUtil.isHexNumber(trimmed)) {
            try {
                return decryptHex(privateKeyHex, trimmed);
            } catch (Exception ignored) {
                // 解密不成功则按原样作为明文（避免极端情况下普通密码被误判）
            }
        }
        return trimmed;
    }
}

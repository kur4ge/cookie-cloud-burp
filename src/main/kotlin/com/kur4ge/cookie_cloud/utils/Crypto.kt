package com.kur4ge.cookie_cloud.utils

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

/**
 * SECP256K1 加密工具
 * 提供基于 secp256k1 曲线的密钥生成、加密和解密功能
 */
class Crypto {
    companion object {
        init {
            // 注册 BouncyCastle 提供者
            Security.addProvider(BouncyCastleProvider())
        }

        private const val CURVE_NAME = "secp256k1"
        private const val KEY_ALGORITHM = "ECDSA"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding"
        private const val CIPHER_ALGORITHM_NOPADDING = "AES/CBC/NoPadding"
        private const val AES_ALGORITHM = "AES"
        // 禁用 HTML 转义，这样等号等特殊字符不会被转义
        private val gson = GsonBuilder().disableHtmlEscaping().create()

        /**
         * 密钥对数据类
         */
        data class KeyPair(
            val privateKey: String,
            val publicKey: String
        )

        /**
         * 生成 secp256k1 密钥对
         * @return 包含私钥和公钥的对象
         */
        fun generateKeyPair(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, "BC")
            keyPairGenerator.initialize(ECGenParameterSpec(CURVE_NAME), SecureRandom())
            val keyPair = keyPairGenerator.generateKeyPair()

            // 获取私钥的原始数据（D值）
            val privateKeyECKey = keyPair.private as org.bouncycastle.jce.interfaces.ECPrivateKey
            val privateKeyBytes = privateKeyECKey.d.toByteArray().let {
                if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else it.padStart(32)
            }

            // 获取公钥的原始数据（Q点的编码）
            val publicKeyECKey = keyPair.public as org.bouncycastle.jce.interfaces.ECPublicKey
            val publicKeyBytes = publicKeyECKey.q.getEncoded(true)
            
            return KeyPair(
                privateKey = Hex.toHexString(privateKeyBytes),
                publicKey = Hex.toHexString(publicKeyBytes)
            )
        }

        /**
         * 从私钥恢复密钥对
         * @param privateKey 私钥（十六进制字符串）
         * @return 包含私钥和公钥的对象
         */
        fun getKeyPairFromPrivateKey(privateKey: String): KeyPair {
            val ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
            val privateKeyBytes = Hex.decode(privateKey)
            val privateKeyValue = BigInteger(1, privateKeyBytes)

            // 从私钥生成公钥点
            val point = ecSpec.g.multiply(privateKeyValue)
            
            // 获取压缩格式的公钥
            val publicKeyBytes = point.getEncoded(true)

            return KeyPair(
                privateKey = privateKey,
                publicKey = Hex.toHexString(publicKeyBytes)
            )
        }

        private fun ByteArray.padStart(length: Int): ByteArray {
            return if (this.size >= length) {
                this
            } else {
                val result = ByteArray(length)
                System.arraycopy(this, 0, result, length - this.size, this.size)
                result
            }
        }
        /**
         * 使用公钥加密数据并用私钥签名
         * @param recipientPublicKey 接收者的公钥
         * @param senderPrivateKey 发送者的私钥（用于签名）
         * @param data 要加密的数据
         * @return 加密并签名后的数据（JSON字符串）
         */
        fun encryptAndSign(recipientPublicKey: String, senderPrivateKey: String, data: String): String {
            // 为当前加密会话生成临时密钥对
            val ephemeral = generateKeyPair()

            // 计算共享密钥
            val sharedSecret = deriveSharedSecret(ephemeral.privateKey, recipientPublicKey)
            val sharedSecretKey = sharedSecret.padStart(64, '0')

            // 使用共享密钥作为 AES 密钥来加密数据
            val keyBytes = Hex.decode(sharedSecretKey)
            val secretKey = SecretKeySpec(keyBytes, AES_ALGORITHM)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC")

            val ivBytes = ByteArray(16)
            System.arraycopy(keyBytes, 0, ivBytes, 0, 16)
            val ivSpec = IvParameterSpec(ivBytes)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            val encryptedData = Base64.getEncoder().encodeToString(encryptedBytes)

            // 创建要签名的数据
            val dataToSignObj = JsonObject()
            dataToSignObj.addProperty("ephPubKey", ephemeral.publicKey)
            dataToSignObj.addProperty("data", encryptedData)
            val dataToSign = gson.toJson(dataToSignObj)

            // 使用发送者的私钥签名数据
            val signature = sign(senderPrivateKey, dataToSign)

            // 组合临时公钥、加密数据和签名
            val resultObj = JsonObject()
            resultObj.addProperty("ephPubKey", ephemeral.publicKey)
            resultObj.addProperty("data", encryptedData)
            resultObj.addProperty("signature", signature)
            return gson.toJson(resultObj)
        }

        /**
         * 验证签名并解密数据
         * @param recipientPrivateKey 接收者的私钥（用于解密）
         * @param senderPublicKey 发送者的公钥（用于验证签名）
         * @param encryptedData 加密的数据（由encryptAndSign函数生成的JSON字符串）
         * @return 解密后的原始数据，如果签名无效则抛出错误
         */
        fun verifyAndDecrypt(recipientPrivateKey: String, senderPublicKey: String, encryptedData: String): String {
            // 解析加密数据
            val parsedData = gson.fromJson(encryptedData, JsonObject::class.java)
            val ephPubKey = parsedData.get("ephPubKey").asString
            val data = parsedData.get("data").asString
            val signature = parsedData.get("signature").asString

            // 验证签名
            val dataToVerifyObj = JsonObject()
            dataToVerifyObj.addProperty("ephPubKey", ephPubKey)
            dataToVerifyObj.addProperty("data", data)
            val dataToVerify = gson.toJson(dataToVerifyObj)

            val isValid = verify(senderPublicKey, dataToVerify, signature)
            if (!isValid) {
                throw SecurityException("签名验证失败，数据可能被篡改或不是由声称的发送者发送")
            }

            // 计算共享密钥
            val sharedSecret = deriveSharedSecret(recipientPrivateKey, ephPubKey)
            val sharedSecretKey = sharedSecret.padStart(64, '0')

            // 使用共享密钥解密数据
            val keyBytes = Hex.decode(sharedSecretKey)
            val secretKey = SecretKeySpec(keyBytes, AES_ALGORITHM)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC")
            val ivBytes = ByteArray(16)
            System.arraycopy(keyBytes, 0, ivBytes, 0, 16)
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = Base64.getDecoder().decode(data)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return String(decryptedBytes)
        }

        /**
         * 使用私钥签名数据
         * @param privateKey 签名者的私钥
         * @param data 要签名的数据
         * @return 签名（Base64字符串）
         */
        fun sign(privateKey: String, data: String): String {
            val ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
            val privateKeyBytes = Hex.decode(privateKey)
            val privateKeyValue = BigInteger(1, privateKeyBytes)

            val privateKeySpec = ECPrivateKeySpec(privateKeyValue, ecSpec)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, "BC")
            val privateKeyObj = keyFactory.generatePrivate(privateKeySpec)

            val signature = Signature.getInstance(SIGNATURE_ALGORITHM, "BC")
            signature.initSign(privateKeyObj)
            signature.update(data.toByteArray())
            val signatureBytes = signature.sign()

            return Base64.getEncoder().encodeToString(signatureBytes)
        }

        /**
         * 验证签名
         * @param publicKey 签名者的公钥
         * @param data 原始数据
         * @param signature 签名（Base64字符串）
         * @return 签名是否有效
         */
        fun verify(publicKey: String, data: String, signature: String): Boolean {
            val ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
            val publicKeyBytes = Hex.decode(publicKey)
            
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, "BC")
            val publicKeyObj = keyFactory.generatePublic(ECPublicKeySpec(ecSpec.curve.decodePoint(publicKeyBytes), ecSpec))

            val signatureObj = Signature.getInstance(SIGNATURE_ALGORITHM, "BC")
            signatureObj.initVerify(publicKeyObj)
            signatureObj.update(data.toByteArray())

            val signatureBytes = Base64.getDecoder().decode(signature)
            return signatureObj.verify(signatureBytes)
        }

        /**
         * 使用多个公钥加密数据并用私钥签名
         * @param recipientPublicKeys 多个接收者的公钥数组
         * @param senderPrivateKey 发送者的私钥（用于签名）
         * @param data 要加密的数据
         * @return 加密并签名后的数据（JSON字符串）
         */
        fun encryptAndSignForMultipleRecipients(
            recipientPublicKeys: List<String>,
            senderPrivateKey: String,
            data: String
        ): String {
            // 为当前加密会话生成临时密钥对
            val ephemeral = generateKeyPair()
            val ephPubKey = ephemeral.publicKey

            // 生成随机AES密钥
            val aesKey = ByteArray(32)
            SecureRandom().nextBytes(aesKey)

            // 使用AES密钥加密数据（只加密一次）
            val secretKey = SecretKeySpec(aesKey, AES_ALGORITHM)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC")
            val ivBytes = ByteArray(16)
            System.arraycopy(aesKey, 0, ivBytes, 0, 16)
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            val encryptedData = Base64.getEncoder().encodeToString(encryptedBytes)

            // 为每个接收者加密AES密钥，使用MD5哈希作为键
            val shareKeys = mutableMapOf<String, String>()

            for (publicKey in recipientPublicKeys) {
                // 计算共享密钥
                val sharedSecret = deriveSharedSecret(ephemeral.privateKey, publicKey)
                val sharedSecretKey = sharedSecret.padStart(64, '0')

                // 使用共享密钥加密AES密钥
                val keyBytes = Hex.decode(sharedSecretKey)
                val sharedSecretKeySpec = SecretKeySpec(keyBytes, AES_ALGORITHM)
                val cipherForKey = Cipher.getInstance(CIPHER_ALGORITHM_NOPADDING, "BC")

                val ivBytesForKey = ByteArray(16)
                System.arraycopy(keyBytes, 0, ivBytesForKey, 0, 16)
                val ivSpecForKey = IvParameterSpec(ivBytesForKey)
    
                cipherForKey.init(Cipher.ENCRYPT_MODE, sharedSecretKeySpec, ivSpecForKey)
                val encryptedKeyBytes = cipherForKey.doFinal(aesKey)
                val encryptedKey = Base64.getEncoder().encodeToString(encryptedKeyBytes)

                // 使用MD5哈希作为键，避免暴露公钥信息
                val md = MessageDigest.getInstance("MD5")
                val keyHash = Hex.toHexString(md.digest((ephPubKey + publicKey).toByteArray()))

                // 存储加密后的密钥，以哈希值为索引
                shareKeys[keyHash] = encryptedKey
            }

            // 创建要签名的数据
            val dataToSignObj = JsonObject()
            dataToSignObj.addProperty("ephPubKey", ephPubKey)
            dataToSignObj.addProperty("data", encryptedData)
            val dataToSign = gson.toJson(dataToSignObj)

            // 使用发送者的私钥签名数据
            val signature = sign(senderPrivateKey, dataToSign)

            // 组合临时公钥、加密数据、加密的密钥和签名
            val resultObj = JsonObject()
            resultObj.addProperty("ephPubKey", ephPubKey)
            resultObj.addProperty("data", encryptedData)
            
            // 添加shareKeys对象
            val shareKeysObj = JsonObject()
            for ((key, value) in shareKeys) {
                shareKeysObj.addProperty(key, value)
            }
            resultObj.add("shareKeys", shareKeysObj)
            
            resultObj.addProperty("signature", signature)
            return gson.toJson(resultObj)
        }

        /**
         * 验证签名并解密多接收者加密数据
         * @param recipientPrivateKey 接收者的私钥（用于解密）
         * @param senderPublicKey 发送者的公钥（用于验证签名）
         * @param encryptedData 加密的数据（由encryptAndSignForMultipleRecipients函数生成的JSON字符串）
         * @return 解密后的原始数据，如果签名无效则抛出错误
         */
        fun verifyAndDecryptForMultipleRecipients(
            recipientPrivateKey: String,
            senderPublicKey: String,
            encryptedData: String
        ): String {
            // 从私钥派生公钥
            val recipientPublicKey = getKeyPairFromPrivateKey(recipientPrivateKey).publicKey

            // 解析加密数据
            val parsedData = gson.fromJson(encryptedData, JsonObject::class.java)
            val ephPubKey = parsedData.get("ephPubKey").asString
            val data = parsedData.get("data").asString
            val shareKeys = parsedData.get("shareKeys").asJsonObject
            val signature = parsedData.get("signature").asString

            // 验证签名
            val dataToVerifyObj = JsonObject()
            dataToVerifyObj.addProperty("ephPubKey", ephPubKey)
            dataToVerifyObj.addProperty("data", data)
            val dataToVerify = gson.toJson(dataToVerifyObj)

            val isValid = verify(senderPublicKey, dataToVerify, signature)
            if (!isValid) {
                throw SecurityException("签名验证失败，数据可能被篡改或不是由声称的发送者发送")
            }

            // 计算接收者的密钥哈希
            val md = MessageDigest.getInstance("MD5")
            val keyHash = Hex.toHexString(md.digest((ephPubKey + recipientPublicKey).toByteArray()))

            // 检查当前接收者的哈希是否在加密密钥列表中
            if (!shareKeys.has(keyHash)) {
                throw SecurityException("当前接收者不在允许解密的列表中")
            }

            // 计算共享密钥
            val sharedSecret = deriveSharedSecret(recipientPrivateKey, ephPubKey)
            val sharedSecretKey = sharedSecret.padStart(64, '0')

            // 解密AES密钥
            val keyBytes = Hex.decode(sharedSecretKey)
            val secretKey = SecretKeySpec(keyBytes, AES_ALGORITHM)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM_NOPADDING, "BC")

            val ivBytes = ByteArray(16)
            System.arraycopy(keyBytes, 0, ivBytes, 0, 16)
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val encryptedKeyBytes = Base64.getDecoder().decode(shareKeys.get(keyHash).asString)
            val decryptedKey = cipher.doFinal(encryptedKeyBytes)

            // 使用解密后的AES密钥解密数据
            val aesKey = SecretKeySpec(decryptedKey, AES_ALGORITHM)
            val cipherForData = Cipher.getInstance(CIPHER_ALGORITHM, "BC")
            val ivBytesForData = ByteArray(16)
            System.arraycopy(decryptedKey, 0, ivBytesForData, 0, 16)
            val ivSpecForData = IvParameterSpec(ivBytesForData)

            cipherForData.init(Cipher.DECRYPT_MODE, aesKey, ivSpecForData)
            val encryptedDataBytes = Base64.getDecoder().decode(data)
            val decryptedBytes = cipherForData.doFinal(encryptedDataBytes)

            return String(decryptedBytes)
        }

        /**
         * 计算共享密钥
         * @param privateKey 私钥
         * @param publicKey 公钥
         * @return 共享密钥的十六进制字符串
         */
        private fun deriveSharedSecret(privateKey: String, publicKey: String): String {
            val ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
            
            // 解析私钥
            val privateKeyBytes = Hex.decode(privateKey)
            val privateKeyValue = BigInteger(1, privateKeyBytes)
            
            // 解析公钥
            val publicKeyBytes = Hex.decode(publicKey)
            val publicPoint = ecSpec.curve.decodePoint(publicKeyBytes)
            
            // 计算共享点
            val sharedPoint = publicPoint.multiply(privateKeyValue)
            
            // 返回x坐标作为共享密钥
            return sharedPoint.normalize().xCoord.toBigInteger().toString(16)
        }
    }
}
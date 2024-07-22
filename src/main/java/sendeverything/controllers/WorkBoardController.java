package sendeverything.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.Random;

@CrossOrigin(origins = "*", maxAge = 3600)
//@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController

@RequestMapping("/api/auth")
public class WorkBoardController {
    @GetMapping("/test")
    public String test() {

        BigInteger p = new BigInteger("2410312426921032588552076022197566074856950548502459942654116941958108831682612228890093858261341614673227141477904012196503648957050582631942730706805009223062734745341073406696246014589361659774041027169249453200378729434170325843778659198143763193776859869524088940195577346119843545301547043747207749969763750084308926339295559968882457872412993810129130294592999947926365264059284647209730384947211681434464714438488520940127459844288859336526896320919633919");
        BigInteger g = new BigInteger("2");

        KeyPair[] keyPairs = new KeyPair[5];
        for (int i = 0; i < 5; i++) {
            BigInteger privateKey = generatePrivateKey(256);
            BigInteger publicKey = generatePublicKey(privateKey, p, g);
            BigInteger sharedSecretKey = generateSharedSecretKey(publicKey, privateKey, p);
            keyPairs[i] = new KeyPair("Owner"+i, publicKey, privateKey);
        }
        System.out.println("====================================");
        calculateEFGH(p, keyPairs);


        return "123";
    }

    public class KeyPair {
        private String owner;
        private BigInteger publicKey;
        private BigInteger privateKey;

        // 建構函式
        public KeyPair(String owner, BigInteger publicKey, BigInteger privateKey) {
            this.owner = owner;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        // getter 和 setter 方法
        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public BigInteger getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(BigInteger publicKey) {
            this.publicKey = publicKey;
        }

        public BigInteger getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(BigInteger privateKey) {
            this.privateKey = privateKey;
        }
    }

    public static BigInteger generatePrivateKey(int bits) {
        Random random = new Random();
        BigInteger privateKey = BigInteger.ZERO;
        for (int i = 0; i < bits; i++) {
            if (random.nextDouble() < 0.5) {
                privateKey = privateKey.setBit(i);
            }
        }
        return privateKey;
    }

    public static BigInteger generatePublicKey(BigInteger privateKey, BigInteger p, BigInteger g) {
        return g.modPow(privateKey, p);
    }

    public static BigInteger generateSharedSecretKey(BigInteger publicKey, BigInteger privateKey, BigInteger p) {
        return publicKey.modPow(privateKey, p);
    }

    public static BigInteger calculateABCD(BigInteger p, BigInteger g, KeyPair[] keyPairs, int half) {
        for (int i = 0; i < keyPairs.length - 1; i++) {
            BigInteger publicKey = keyPairs[i + 1].getPublicKey();
            BigInteger privateKey = keyPairs[i].getPrivateKey();
            g = g.multiply(publicKey.modPow(privateKey, p)).mod(p);
        }
        return g;
    }

    public static void calculateEFGH(BigInteger p, KeyPair[] keyPairs) {

        for (int i = 0; i < keyPairs.length; i++) { //8
            BigInteger result = BigInteger.ONE; // 初始結果為1
            for (int j = i; j < i + keyPairs.length; j++) { //6

                if (i != j) { // 略過自己與自己計算共享密鑰
                    if(j == i + 1) {
                        int nextIndex = (i + 1) % keyPairs.length;
                        int nextIndex2 = (i + 2) % keyPairs.length;
//                        result = result.multiply(keyPairs[nextIndex].getPublicKey().modPow(keyPairs[nextIndex2].getPrivateKey(), p)).mod(p);
                        result = keyPairs[nextIndex].getPublicKey();
                        result = result.modPow(keyPairs[nextIndex2].getPrivateKey(), p);
//                        System.out.println("nextIndex: " + nextIndex);
//                        System.out.println("nextIndex2: " + nextIndex2);
//                        System.out.println("keyPairsOwner->" + keyPairs[i].getOwner());
                    } else {
                        int index = ( j ) % keyPairs.length;
                        int index2 = ( i + 2 ) % keyPairs.length;
                        if(index != i & index != index2) {
                            result = result.modPow(keyPairs[index].getPrivateKey(), p);
//                            System.out.println("index: " + index); // good
//                            System.out.println("keyPairsOwner->" + keyPairs[i].getOwner());
                        }
                    }
                }

            }
            System.out.println("ownnnnnnnnnnnnnnnnnnnnner: " + keyPairs[i].getOwner());
            System.out.println("Shared secret key between " + result.modPow(keyPairs[i].getPrivateKey(), p));
        }
    }
}

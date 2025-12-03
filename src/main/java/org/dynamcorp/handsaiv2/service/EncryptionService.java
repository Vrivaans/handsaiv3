package org.dynamcorp.handsaiv2.service;

public interface EncryptionService {
    String encrypt(String data);

    String decrypt(String encryptedData);
}

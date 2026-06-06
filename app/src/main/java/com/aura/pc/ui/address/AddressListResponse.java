package com.aura.pc.ui.address;

import java.util.List;

/**
 * Bao bọc phản hồi của các endpoint /auth/addresses.
 * Backend luôn trả về { success, addresses[], message? }.
 */
public class AddressListResponse {
    public boolean success;
    public List<Address> addresses;
    public String message;
}

package com.example.aura_pc_app.data.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Chuyển bất kỳ giá trị JSON nào (object, array hoặc primitive) thành chuỗi JSON thô.
 *
 * <p>API trả về {@code specs} là một object (vd: {@code {"CPU":"...","RAM":"..."}}) và
 * {@code images} là một mảng (vd: {@code ["url1","url2"]}). Trong khi đó
 * {@link com.example.aura_pc_app.data.db.entity.ProductEntity} khai báo các trường này là
 * {@code String}. Nếu không xử lý, Gson sẽ ném lỗi "Expected a string but was BEGIN_OBJECT/
 * BEGIN_ARRAY" khiến TOÀN BỘ danh sách sản phẩm parse thất bại — app rơi về dữ liệu demo
 * (10 sản phẩm) nên mọi từ khoá tìm kiếm chỉ ra được 1-2 kết quả.
 *
 * <p>Deserializer này giữ nguyên dữ liệu dưới dạng chuỗi JSON để lưu vào Room và phục vụ
 * tìm kiếm/hiển thị (cleanSpecs đã được thiết kế để bóc tách chuỗi JSON này).
 */
public class RawJsonStringDeserializer implements JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.isJsonPrimitive()) {
            // Chuỗi/số/boolean -> lấy giá trị nguyên bản, không bọc thêm dấu nháy.
            return json.getAsString();
        }
        // Object hoặc Array -> giữ nguyên dạng chuỗi JSON.
        return json.toString();
    }
}

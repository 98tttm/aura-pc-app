package com.aura.pc.ui.hub;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.aura.pc.utils.AuraMapUtils;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuraHubComposeBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "AuraHubComposeSheet";
    private static final int MAX_IMAGE_SIDE = 800;
    private static final int JPEG_QUALITY = 60;
    private static final int MAX_IMAGES = 3;

    private EditText input;
    private View previewWrap;
    private LinearLayout previewContainer;
    private ProgressBar loading;
    private TextView submit;
    private TextView imageHint;
    private Runnable onPosted;

    private final List<PickedImage> pickedItems = new ArrayList<>();
    private final Map<View, PickedImage> viewToImage = new HashMap<>();

    private ActivityResultLauncher<PickVisualMediaRequest> imagePicker;

    /** Holds one selected image and its base64-encoded payload. */
    private static final class PickedImage {
        final Uri uri;
        final String base64;
        PickedImage(Uri uri, String base64) {
            this.uri = uri;
            this.base64 = base64;
        }
    }

    public static void show(@NonNull FragmentManager fm, @Nullable Runnable onPosted) {
        AuraHubComposeBottomSheet sheet = new AuraHubComposeBottomSheet();
        sheet.onPosted = onPosted;
        sheet.show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES),
                uris -> {
                    if (uris != null && !uris.isEmpty()) onImagesPicked(uris);
                });
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_AuraHubCompose;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetBehavior<?> behavior = dialog.getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.88f);
        params.height = height;
        dialog.getWindow().setAttributes(params);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_aura_hub_compose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        input = view.findViewById(R.id.compose_input);
        loading = view.findViewById(R.id.compose_loading);
        submit = view.findViewById(R.id.compose_submit);
        previewWrap = view.findViewById(R.id.compose_preview_wrap);
        previewContainer = view.findViewById(R.id.compose_preview_container);
        imageHint = view.findViewById(R.id.compose_image_hint);

        view.findViewById(R.id.compose_cancel).setOnClickListener(v -> dismiss());
        submit.setOnClickListener(v -> submitPost());
        view.findViewById(R.id.compose_pick_image).setOnClickListener(v -> {
            if (imagePicker != null) {
                imagePicker.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia
                                        .ImageOnly.INSTANCE)
                                .build());
            }
        });
        bindAuthor(view);
        input.requestFocus();
        input.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        }, 180);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pickedItems.clear();
        viewToImage.clear();
        if (previewContainer != null) previewContainer.removeAllViews();
    }

    private void bindAuthor(View root) {
        TextView author = root.findViewById(R.id.compose_author);
        TextView avatar = root.findViewById(R.id.compose_avatar);
        String json = TokenManager.getInstance(requireContext()).getCurrentUserJson();
        if (json == null || json.trim().isEmpty()) return;
        try {
            Map<String, Object> user = new Gson().fromJson(json, Map.class);
            String name = AuraMapUtils.displayName(user);
            if (name != null && !name.trim().isEmpty()) {
                if (author != null) author.setText(name);
                if (avatar != null && !name.isEmpty()) avatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void onImagesPicked(@NonNull List<Uri> uris) {
        Context ctx = requireContext();
        ContentResolver resolver = ctx.getContentResolver();
        int slots = Math.min(uris.size(), MAX_IMAGES - pickedItems.size());
        if (slots <= 0) {
            Toast.makeText(ctx, getString(R.string.hub_compose_max_images, MAX_IMAGES), Toast.LENGTH_SHORT).show();
            return;
        }
        boolean anyFailed = false;
        for (int i = 0; i < slots; i++) {
            Uri uri = uris.get(i);
            try {
                Bitmap bitmap = decodeScaled(resolver, uri);
                if (bitmap == null) {
                    anyFailed = true;
                    continue;
                }
                String base64 = bitmapToBase64(bitmap);
                bitmap.recycle();
                PickedImage item = new PickedImage(uri, base64);
                pickedItems.add(item);
                addPreviewItem(item);
            } catch (IOException e) {
                anyFailed = true;
            }
        }
        if (uris.size() > slots) {
            Toast.makeText(ctx, getString(R.string.hub_compose_max_images, MAX_IMAGES), Toast.LENGTH_SHORT).show();
        } else if (anyFailed) {
            Toast.makeText(ctx, R.string.hub_compose_image_failed, Toast.LENGTH_SHORT).show();
        }
        refreshPreviewVisibility();
    }

    private void addPreviewItem(@NonNull PickedImage item) {
        if (previewContainer == null) return;
        View itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_compose_preview, previewContainer, false);
        ImageView thumb = itemView.findViewById(R.id.preview_thumb);
        thumb.setImageURI(item.uri);
        TextView remove = itemView.findViewById(R.id.preview_remove);
        remove.setOnClickListener(v -> removePickedImage(item, itemView));
        previewContainer.addView(itemView);
        viewToImage.put(itemView, item);
    }

    private void removePickedImage(@NonNull PickedImage item, @Nullable View itemView) {
        pickedItems.remove(item);
        if (itemView != null) {
            previewContainer.removeView(itemView);
            viewToImage.remove(itemView);
        } else if (previewContainer != null) {
            for (Map.Entry<View, PickedImage> entry : new HashMap<>(viewToImage).entrySet()) {
                if (entry.getValue() == item) {
                    previewContainer.removeView(entry.getKey());
                    viewToImage.remove(entry.getKey());
                }
            }
        }
        refreshPreviewVisibility();
    }

    private void refreshPreviewVisibility() {
        boolean has = !pickedItems.isEmpty();
        if (previewWrap != null) previewWrap.setVisibility(has ? View.VISIBLE : View.GONE);
        if (imageHint != null) {
            if (pickedItems.size() > 1) {
                imageHint.setText(getString(R.string.hub_compose_images_picked, pickedItems.size()));
            } else if (pickedItems.size() == 1) {
                imageHint.setText(R.string.hub_compose_image_picked);
            } else {
                imageHint.setText(R.string.hub_compose_image_hint);
            }
        }
    }

    private Bitmap decodeScaled(ContentResolver resolver, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int srcW = bounds.outWidth;
        int srcH = bounds.outHeight;
        int sample = 1;
        int longer = Math.max(srcW, srcH);
        while (longer / sample > MAX_IMAGE_SIDE * 2) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap raw;
        try (InputStream in = resolver.openInputStream(uri)) {
            raw = BitmapFactory.decodeStream(in, null, opts);
        }
        if (raw == null) return null;
        Bitmap scaled = scaleToMax(raw, MAX_IMAGE_SIDE);
        if (scaled != raw) raw.recycle();
        return scaled;
    }

    private Bitmap scaleToMax(Bitmap src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSide && h <= maxSide) return src;
        float ratio = (float) maxSide / Math.max(w, h);
        int targetW = Math.round(w * ratio);
        int targetH = Math.round(h * ratio);
        Bitmap scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true);
        return scaled;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        byte[] bytes = out.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private void submitPost() {
        String content = input.getText().toString().trim();
        boolean hasImage = !pickedItems.isEmpty();
        if (content.isEmpty() && !hasImage) {
            Toast.makeText(requireContext(), R.string.hub_post_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        if (!hasImage) {
            submitCreatePost(content, new ArrayList<>());
            return;
        }
        // Multipart upload first → returned URLs feed into /api/hub/posts JSON.
        final String postContent = content;
        List<MultipartBody.Part> parts = new ArrayList<>();
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            for (int i = 0; i < pickedItems.size(); i++) {
                Uri uri = pickedItems.get(i).uri;
                if (uri == null) continue;
                byte[] bytes = readBytesFromUri(resolver, uri);
                if (bytes == null || bytes.length == 0) continue;
                RequestBody body = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
                parts.add(MultipartBody.Part.createFormData("images", "img_" + i + ".jpg", body));
            }
        } catch (IOException ioe) {
            setLoading(false);
            Toast.makeText(requireContext(), R.string.hub_compose_image_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (parts.isEmpty()) {
            submitCreatePost(postContent, new ArrayList<>());
            return;
        }
        ApiClient.getInstance(requireContext()).getApiService()
                .uploadHubImages(parts)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (!response.isSuccessful()) {
                            setLoading(false);
                            String err = null;
                            if (response.errorBody() != null) {
                                try { err = response.errorBody().string(); }
                                catch (IOException | RuntimeException ignored) { err = null; }
                            }
                            showSubmitError(response.code(), err);
                            return;
                        }
                        List<String> urls = extractUploadUrls(response.body());
                        submitCreatePost(postContent, urls);
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(requireContext(), R.string.hub_post_network_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void submitCreatePost(String content, List<String> imageUrls) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        if (!imageUrls.isEmpty()) {
            body.put("images", imageUrls);
        }
        ApiClient.getInstance(requireContext()).getApiService()
                .createHubPost(body)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), R.string.hub_post_created, Toast.LENGTH_SHORT).show();
                            if (onPosted != null) onPosted.run();
                            dismiss();
                        } else {
                            String err = null;
                            if (response.errorBody() != null) {
                                try { err = response.errorBody().string(); }
                                catch (IOException | RuntimeException ignored) { err = null; }
                            }
                            showSubmitError(response.code(), err);
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(requireContext(), R.string.hub_post_network_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private byte[] readBytesFromUri(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractUploadUrls(Map<String, Object> body) {
        List<String> out = new ArrayList<>();
        if (body == null) return out;
        Object urls = body.get("urls");
        if (urls instanceof List) {
            for (Object o : (List<Object>) urls) {
                if (o != null) out.add(o.toString());
            }
        }
        if (out.isEmpty()) {
            Object data = body.get("data");
            if (data instanceof Map) {
                Object inner = ((Map<String, Object>) data).get("urls");
                if (inner instanceof List) {
                    for (Object o : (List<Object>) inner) {
                        if (o != null) out.add(o.toString());
                    }
                }
            }
        }
        return out;
    }

    private void showSubmitError(int httpCode, @Nullable String errorBody) {
        String msg = extractErrorMessage(errorBody);
        if (httpCode == 401 || httpCode == 403) {
            Toast.makeText(requireContext(), R.string.hub_post_unauthorized, Toast.LENGTH_LONG).show();
            return;
        }
        if (msg != null && !msg.isEmpty()) {
            Toast.makeText(requireContext(),
                    getString(R.string.hub_post_failed_reason, msg),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), R.string.hub_post_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Pull a friendly message out of common server JSON shapes: {"message":..}, {"error":..}, raw text. */
    @Nullable
    private String extractErrorMessage(@Nullable String errorBody) {
        if (errorBody == null) return null;
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(errorBody);
            if (el != null && el.isJsonObject()) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                if (obj.has("message")) return obj.get("message").getAsString();
                if (obj.has("error")) return obj.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
        }
        String trimmed = errorBody.trim();
        if (trimmed.length() > 140) trimmed = trimmed.substring(0, 140) + "...";
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setLoading(boolean isLoading) {
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        submit.setEnabled(!isLoading);
        input.setEnabled(!isLoading);
    }
}

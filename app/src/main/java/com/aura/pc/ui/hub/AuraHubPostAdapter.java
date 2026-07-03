package com.aura.pc.ui.hub;

import android.content.Context;
import android.text.TextUtils;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.aura.pc.utils.AuraMapUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.utils.Constants;
import com.example.aura_pc_app.utils.perf.AuraColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class AuraHubPostAdapter extends RecyclerView.Adapter<AuraHubPostAdapter.PostViewHolder> {
    interface Listener {
        void onLike(Map<String, Object> post);
        void onFollow(Map<String, Object> author);
        void onComment(Map<String, Object> post);
        void onImageClick(List<String> imageUrls, int clickedIndex);
    }

    private final List<Map<String, Object>> posts = new ArrayList<>();
    private final Listener listener;
    private Set<String> followingIds = new HashSet<>();
    private String currentUserId = "";

    AuraHubPostAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitPosts(List<Map<String, Object>> next) {
        posts.clear();
        if (next != null) posts.addAll(next);
        notifyDataSetChanged();
    }

    void setFollowingIds(Set<String> ids) {
        followingIds = ids == null ? new HashSet<>() : new HashSet<>(ids);
        notifyDataSetChanged();
    }

    void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId == null ? "" : currentUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_aura_hub_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releaseCarousel();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImage;
        private final ImageView postImage;
        private final FrameLayout postImagesGrid;
        private final TextView authorText;
        private final TextView timeText;
        private final TextView topicText;
        private final TextView contentText;
        private final TextView statsText;
        private final TextView followPlus;
        private final TextView avatarInitial;
        private final ImageButton likeButton;
        private final ImageButton commentButton;

        // Carousel state (multi-image path)
        private ViewPager2 carouselPager;
        private LinearLayout carouselDots;
        private TextView carouselCounter;
        private final List<ViewPager2.OnPageChangeCallback> activeCallbacks = new ArrayList<>();
        private int currentPagerIndex = 0;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.hub_post_avatar);
            postImage = itemView.findViewById(R.id.hub_post_image);
            postImagesGrid = itemView.findViewById(R.id.hub_post_images_stack);
            authorText = itemView.findViewById(R.id.hub_post_author);
            timeText = itemView.findViewById(R.id.hub_post_time);
            topicText = itemView.findViewById(R.id.hub_post_topic);
            contentText = itemView.findViewById(R.id.hub_post_content);
            statsText = itemView.findViewById(R.id.hub_post_stats);
            followPlus = itemView.findViewById(R.id.hub_post_follow_plus);
            avatarInitial = itemView.findViewById(R.id.hub_post_avatar_initial);
            likeButton = itemView.findViewById(R.id.hub_post_like);
            commentButton = itemView.findViewById(R.id.hub_post_comment);
        }

        void bind(Map<String, Object> post) {
            Map<String, Object> author = AuraMapUtils.mapValue(post.get("author"));
            String authorId = AuraMapUtils.userId(author);
            String authorName = AuraMapUtils.displayName(author);
            authorText.setText(authorName);
            timeText.setText(timeAgo(AuraMapUtils.firstString(post, "createdAt", "updatedAt")));

            String topic = AuraMapUtils.firstString(post, "topic");
            topicText.setVisibility(topic.isEmpty() ? View.GONE : View.VISIBLE);
            topicText.setText(topic.isEmpty() ? "" : "#" + topic);

            String content = AuraMapUtils.firstString(post, "content", "text", "body");
            contentText.setText(AuraMapUtils.compact(content, 360));

            bindAvatar(resolveUrl(AuraMapUtils.avatarUrl(author)), authorName);
            bindImages(post);

            int likes = AuraMapUtils.intValue(post.get("likeCount"));
            int comments = AuraMapUtils.intValue(post.get("commentCount"));
            int shares = AuraMapUtils.intValue(post.get("shareCount"));
            statsText.setText(itemView.getContext().getString(
                    R.string.hub_post_stats_format, likes, comments, shares));

            boolean liked = Boolean.TRUE.equals(post.get("liked"))
                    || AuraMapUtils.containsId(post.get("likes"), currentUserId);
            likeButton.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            likeButton.setImageTintList(ColorStateList.valueOf(liked ? AuraColors.parse("#FF3040") : AuraColors.parse("#111111")));
            likeButton.setOnClickListener(v -> listener.onLike(post));
            commentButton.setOnClickListener(v -> listener.onComment(post));
            itemView.setOnClickListener(v -> listener.onComment(post));

            boolean ownPost = !TextUtils.isEmpty(currentUserId) && currentUserId.equals(authorId);
            boolean following = followingIds.contains(authorId);
            followPlus.setVisibility(ownPost || authorId.isEmpty() || following ? View.GONE : View.VISIBLE);
            followPlus.setOnClickListener(v -> listener.onFollow(author));
        }

        private void bindAvatar(String url, String authorName) {
            if (url == null || url.isEmpty()) {
                avatarImage.setVisibility(View.GONE);
                avatarInitial.setVisibility(View.VISIBLE);
                avatarInitial.setText(initial(authorName));
                return;
            }
            avatarInitial.setVisibility(View.GONE);
            avatarImage.setVisibility(View.VISIBLE);
            avatarImage.setPadding(0, 0, 0, 0);
            Glide.with(itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.bg_hub_avatar_circle)
                    .error(R.drawable.bg_hub_avatar_circle)
                    .apply(RequestOptions.circleCropTransform())
                    .into(avatarImage);
        }

        /**
         * Renders 0, 1, or N images on the post:
         *   0  -> both containers gone
         *   1  -> hub_post_image (220dp, wrap_content width, fitCenter, centered)
         *   N>=2 -> horizontal 4:5 carousel via ViewPager2 (tile bounded ~85% screen)
         *           + counter "i/N" pill top-end + dot strip bottom-center.
         * Tap (single image OR any carousel page) fires
         *   Listener.onImageClick(urls, clickedIndex).
         */
        private void bindImages(Map<String, Object> post) {
            List<String> rawUrls = AuraMapUtils.allImageUrls(post);
            List<String> resolved = new ArrayList<>();
            for (String url : rawUrls) {
                String abs = resolveUrl(url);
                if (abs != null && !abs.isEmpty()) resolved.add(abs);
            }
            // Reset state first to avoid leaks across recycled holders.
            postImage.setVisibility(View.GONE);
            postImage.setOnClickListener(null);
            postImagesGrid.setVisibility(View.GONE);
            postImagesGrid.setOnClickListener(null);
            releaseCarousel();
            postImagesGrid.removeAllViews();
            currentPagerIndex = 0;

            if (resolved.isEmpty()) return;

            if (resolved.size() == 1) {
                postImage.setVisibility(View.VISIBLE);
                postImage.setOnClickListener(v -> listener.onImageClick(resolved, 0));
                bindImage(postImage, resolved.get(0), R.drawable.figma_home_banner);
                return;
            }
            bindCarousel(resolved);
        }

        private void bindCarousel(List<String> resolved) {
            Context ctx = itemView.getContext();
            int screenWidthPx = ctx.getResources().getDisplayMetrics().widthPixels;

            // Bounded tile width: min(85% of screen, screenWidth - 28dp padding)
            int tileWidthPx = Math.min(
                    Math.round(screenWidthPx * 0.85f),
                    screenWidthPx - dpToPx(28));
            int tileHeightPx = Math.round(tileWidthPx * 5f / 4f); // 4:5 portrait

            postImagesGrid.setVisibility(View.VISIBLE);

            // Pager — tile sits centered horizontally at top of FrameLayout.
            carouselPager = new ViewPager2(ctx);
            FrameLayout.LayoutParams pagerLp = new FrameLayout.LayoutParams(
                    tileWidthPx, tileHeightPx,
                    Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            pagerLp.topMargin = 0;
            carouselPager.setLayoutParams(pagerLp);
            carouselPager.setOffscreenPageLimit(Math.min(3, resolved.size()));
            carouselPager.setAdapter(new ImageCarouselAdapter(
                    resolved, tileWidthPx, tileHeightPx, listener));
            postImagesGrid.addView(carouselPager);

            // Counter "i/N" pill, top-end, inline GradientDrawable (no new drawable).
            carouselCounter = new TextView(ctx);
            FrameLayout.LayoutParams counterLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            int m = dpToPx(8);
            counterLp.setMargins(m, m, m, 0);
            carouselCounter.setLayoutParams(counterLp);
            carouselCounter.setText((currentPagerIndex + 1) + "/" + resolved.size());
            carouselCounter.setTextColor(android.graphics.Color.WHITE);
            carouselCounter.setTextSize(11);
            carouselCounter.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            android.graphics.drawable.GradientDrawable pill =
                    new android.graphics.drawable.GradientDrawable();
            pill.setColor(0xCC000000);
            pill.setCornerRadius(dpToPx(10));
            carouselCounter.setBackground(pill);
            postImagesGrid.addView(carouselCounter);

            // Dots strip, bottom-center.
            carouselDots = new LinearLayout(ctx);
            carouselDots.setOrientation(LinearLayout.HORIZONTAL);
            carouselDots.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams dotsLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            dotsLp.setMargins(0, 0, 0, dpToPx(8));
            carouselDots.setLayoutParams(dotsLp);
            postImagesGrid.addView(carouselDots);
            setupIndicators(carouselDots, resolved.size(), 0);

            // Wrapper click → open viewer at currently-swiped index.
            postImagesGrid.setOnClickListener(v ->
                    listener.onImageClick(resolved, currentPagerIndex));

            // Track swipe via OnPageChangeCallback.
            ViewPager2.OnPageChangeCallback cb = new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int position) {
                    currentPagerIndex = position;
                    carouselCounter.setText((position + 1) + "/" + resolved.size());
                    updateIndicators(carouselDots, position);
                }
            };
            carouselPager.registerOnPageChangeCallback(cb);
            activeCallbacks.add(cb);
        }

        private void setupIndicators(LinearLayout container, int count, int activeIndex) {
            container.removeAllViews();
            int dotHeight = dpToPx(6);
            int activeW = dpToPx(22);
            int inactiveW = dpToPx(7);
            int gap = dpToPx(5);
            Context ctx = container.getContext();
            for (int i = 0; i < count; i++) {
                View dot = new View(ctx);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        (i == activeIndex) ? activeW : inactiveW, dotHeight);
                lp.setMarginEnd(i == count - 1 ? 0 : gap);
                dot.setLayoutParams(lp);
                dot.setBackgroundResource(i == activeIndex
                        ? R.drawable.bg_home_banner_indicator_active
                        : R.drawable.bg_home_banner_indicator_inactive);
                container.addView(dot);
            }
        }

        private void updateIndicators(LinearLayout container, int activeIndex) {
            int dotHeight = dpToPx(6);
            int activeW = dpToPx(22);
            int inactiveW = dpToPx(7);
            int gap = dpToPx(5);
            int count = container.getChildCount();
            for (int i = 0; i < count; i++) {
                View dot = container.getChildAt(i);
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) dot.getLayoutParams();
                lp.width = (i == activeIndex) ? activeW : inactiveW;
                lp.height = dotHeight;
                lp.setMarginEnd(i == count - 1 ? 0 : gap);
                dot.setLayoutParams(lp);
                dot.setBackgroundResource(i == activeIndex
                        ? R.drawable.bg_home_banner_indicator_active
                        : R.drawable.bg_home_banner_indicator_inactive);
            }
        }

        void releaseCarousel() {
            if (carouselPager != null && !activeCallbacks.isEmpty()) {
                for (ViewPager2.OnPageChangeCallback cb : activeCallbacks) {
                    carouselPager.unregisterOnPageChangeCallback(cb);
                }
                activeCallbacks.clear();
                carouselPager.setAdapter(null);
            }
            carouselPager = null;
            carouselDots = null;
            carouselCounter = null;
            currentPagerIndex = 0;
        }

        private String initial(String value) {
            if (value == null || value.trim().isEmpty()) return "A";
            return value.trim().substring(0, 1).toUpperCase(Locale.getDefault());
        }

        private void bindImage(ImageView view, String url, int fallbackRes) {
            if (url == null || url.isEmpty()) {
                view.setImageResource(fallbackRes);
                return;
            }
            Glide.with(itemView.getContext())
                    .load(url)
                    .placeholder(fallbackRes)
                    .error(fallbackRes)
                    .into(view);
        }

        private String explicitPostImageUrl(Map<String, Object> post) {
            // Kept for callers that only want the first image URL.
            List<String> all = AuraMapUtils.allImageUrls(post);
            return all.isEmpty() ? "" : all.get(0);
        }

        private String firstMediaUrl(Object value) {
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    String url = firstMediaUrl(item);
                    if (!url.isEmpty()) return url;
                }
                return "";
            }
            if (value instanceof String) {
                String s = ((String) value).trim();
                return s.isEmpty() ? "" : s;
            }
            if (value instanceof Map) {
                Map<String, Object> media = AuraMapUtils.mapValue(value);
                String type = AuraMapUtils.firstString(media, "type", "mimeType", "mime");
                String url = AuraMapUtils.firstString(media, "url", "src", "path");
                if (url.isEmpty()) return "";
                if (type.isEmpty() || type.toLowerCase(Locale.ROOT).contains("image")) return url;
                return "";
            }
            return "";
        }

        private String resolveUrl(String url) {
            if (url == null || url.isEmpty()) return "";
            if (url.startsWith("http")) return url;
            String root = Constants.BASE_URL.replace("/api/", "");
            if (url.startsWith("/")) return root + url;
            return root + "/" + url;
        }

        private String timeAgo(String value) {
            if (value == null || value.isEmpty()) return "";
            Date date = parseDate(value);
            if (date == null) return "";
            long diffSeconds = Math.max(0, (System.currentTimeMillis() - date.getTime()) / 1000);
            if (diffSeconds < 60) return diffSeconds + "s";
            if (diffSeconds < 3600) return (diffSeconds / 60) + "m";
            if (diffSeconds < 86400) return (diffSeconds / 3600) + "h";
            if (diffSeconds < 604800) return (diffSeconds / 86400) + "d";
            return new SimpleDateFormat("d MMM", Locale.US).format(date);
        }

        private Date parseDate(String value) {
            String[] patterns = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
            };
            for (String pattern : patterns) {
                try {
                    return new SimpleDateFormat(pattern, Locale.US).parse(value);
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        private int dpToPx(int dp) {
            return Math.round(dp * itemView.getResources().getDisplayMetrics().density);
        }
    }

    /**
     * Inner adapter for the multi-image carousel (ViewPager2). Each page is a
     * single ImageView scaled FIT_CENTER within a fixed (tileWidthPx × tileHeightPx)
     * cell — so the tile keeps its declared 4:5 aspect regardless of source ratio.
     * Click handling lives on the outer FrameLayout wrapper
     * (hub_post_images_stack) because ViewPager2 swallows the touch gesture.
     */
    private static final class ImageCarouselAdapter
            extends RecyclerView.Adapter<ImageCarouselAdapter.ImageHolder> {

        private final List<String> urls;
        private final int tileWidthPx;
        private final int tileHeightPx;
        private final Listener listener;

        ImageCarouselAdapter(List<String> urls, int tileWidthPx, int tileHeightPx,
                              Listener listener) {
            this.urls = urls;
            this.tileWidthPx = tileWidthPx;
            this.tileHeightPx = tileHeightPx;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            // ViewPager2 requires every page to MATCH_PARENT in both dims; the pager's
            // own frameLayout LayoutParams already constrains the tile to 4:5.
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setBackgroundResource(R.drawable.bg_hub_media);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setClickable(true);
            iv.setFocusable(true);
            return new ImageHolder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageHolder h, int position) {
            Glide.with(h.image.getContext())
                    .load(urls.get(position))
                    .placeholder(R.drawable.figma_home_banner)
                    .error(R.drawable.figma_home_banner)
                    .into(h.image);
            // Tap on the page ImageView (not the outer wrapper) — RecyclerView only
            // swallows gestures interpreted as horizontal drags; taps propagate here.
            final int index = position;
            h.image.setOnClickListener(v -> listener.onImageClick(urls, index));
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static final class ImageHolder extends RecyclerView.ViewHolder {
            final ImageView image;
            ImageHolder(@NonNull ImageView itemView) {
                super(itemView);
                this.image = itemView;
            }
        }
    }
}

package com.aura.pc.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.LoadState;
import androidx.paging.LoadStateAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;

/**
 * Adapter hiển thị trạng thái loading/error ở cuối danh sách (infinite scroll footer).
 */
public class ProductLoadStateAdapter extends LoadStateAdapter<ProductLoadStateAdapter.LoadStateViewHolder> {

    private final Runnable retryCallback;

    public ProductLoadStateAdapter(Runnable retryCallback) {
        this.retryCallback = retryCallback;
    }

    @NonNull
    @Override
    public LoadStateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @NonNull LoadState loadState) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_load_state_footer, parent, false);
        return new LoadStateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoadStateViewHolder holder, @NonNull LoadState loadState) {
        holder.bind(loadState);
    }

    class LoadStateViewHolder extends RecyclerView.ViewHolder {
        private final ProgressBar progressBar;
        private final TextView tvError;
        private final Button btnRetry;

        LoadStateViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBarFooter);
            tvError = itemView.findViewById(R.id.tvErrorFooter);
            btnRetry = itemView.findViewById(R.id.btnRetryFooter);
        }

        void bind(LoadState loadState) {
            if (loadState instanceof LoadState.Loading) {
                progressBar.setVisibility(View.VISIBLE);
                tvError.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
            } else if (loadState instanceof LoadState.Error) {
                progressBar.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Lỗi tải dữ liệu");
                btnRetry.setVisibility(View.VISIBLE);
                btnRetry.setOnClickListener(v -> {
                    if (retryCallback != null) retryCallback.run();
                });
            } else {
                progressBar.setVisibility(View.GONE);
                tvError.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
            }
        }
    }
}

package com.example.aura_pc_app;

import static org.junit.Assert.assertEquals;

import com.example.aura_pc_app.domain.cart.CartItem;
import com.example.aura_pc_app.domain.cart.CartMergeUseCase;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CartMergeUseCaseTest {

    @Test
    public void merge_shouldAddQuantitiesForSameProductAndVariant() {
        CartMergeUseCase useCase = new CartMergeUseCase();

        List<CartItem> merged = useCase.merge(
                Arrays.asList(new CartItem("p1", "v1", 2)),
                Arrays.asList(new CartItem("p1", "v1", 3))
        );

        assertEquals(1, merged.size());
        assertEquals("p1", merged.get(0).productId);
        assertEquals("v1", merged.get(0).variantId);
        assertEquals(5, merged.get(0).quantity);
    }

    @Test
    public void merge_shouldKeepDifferentVariantsSeparate() {
        CartMergeUseCase useCase = new CartMergeUseCase();

        List<CartItem> merged = useCase.merge(
                Arrays.asList(new CartItem("p1", "black", 1)),
                Arrays.asList(new CartItem("p1", "white", 1))
        );

        assertEquals(2, merged.size());
    }
}

"use client";

import * as React from "react";
import { useShopProducts } from "../hooks/use-products";
import { ProductCard } from "./ProductCard";
import { ShopHeader } from "./ShopHeader";
import { Grid } from "@/components/layout/Grid";
import { Stack } from "@/components/layout/Stack";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { AlertCircle, RefreshCw, PackageOpen, Sparkles } from "lucide-react";
import type { TShop } from "@/types/shop";

type ProductGridProps = {
  shopId: string;
  shop?: TShop;
};

export function ProductGrid({ shopId, shop }: ProductGridProps) {
  const {
    data: products,
    isLoading,
    isError,
    error,
    refetch,
  } = useShopProducts(shopId);

  const shopName = shop?.name || "Shop";

  return (
    <Stack gap="xl">
      <ShopHeader shop={shop} totalProducts={products?.length || 0} />

      {isLoading ? (
        <Stack gap="md">
          <div className="flex items-center justify-between">
            <Skeleton className="h-6 w-36" />
            <Skeleton className="h-4 w-20" />
          </div>
          <Grid cols={3} gap="md">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="h-80 rounded-2xl border border-border/40 bg-card/40 p-5 space-y-4"
              >
                <Skeleton className="aspect-video w-full rounded-xl" />
                <div className="space-y-2">
                  <Skeleton className="h-5 w-3/4" />
                  <Skeleton className="h-4 w-full" />
                </div>
                <Skeleton className="h-10 w-full rounded-lg mt-auto" />
              </div>
            ))}
          </Grid>
        </Stack>
      ) : isError ? (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-8 text-center space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-destructive/20 text-destructive">
            <AlertCircle className="h-6 w-6" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-semibold text-foreground">Failed to Load Products</h3>
            <p className="text-xs text-muted-foreground">
              {error instanceof Error ? error.message : "Could not retrieve products for this shop."}
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={() => refetch()} className="gap-2">
            <RefreshCw className="h-3.5 w-3.5" />
            Retry
          </Button>
        </div>
      ) : !products || products.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card/40 p-12 text-center space-y-3">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-secondary text-muted-foreground">
            <PackageOpen className="h-6 w-6" />
          </div>
          <h3 className="text-base font-semibold text-foreground">No Products Listed</h3>
          <p className="text-xs text-muted-foreground max-w-sm mx-auto">
            This vendor currently does not have any active inventory listed in the catalog.
          </p>
        </div>
      ) : (
        <Stack gap="md">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-indigo-400" />
              <h2 className="text-lg font-bold tracking-tight text-foreground">
                All Products
              </h2>
            </div>
            <span className="text-xs text-muted-foreground">
              Showing {products.length} {products.length === 1 ? "item" : "items"}
            </span>
          </div>

          <Grid cols={3} gap="md">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                shopId={shopId}
                shopName={shopName}
              />
            ))}
          </Grid>
        </Stack>
      )}
    </Stack>
  );
}

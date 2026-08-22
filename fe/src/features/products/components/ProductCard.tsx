"use client";

import * as React from "react";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StockBadge } from "./StockBadge";
import { useCartStore } from "@/store/cart-store";
import { formatCurrency } from "@/lib/utils";
import { ShoppingCart, Plus, Minus, Check, Package } from "lucide-react";
import type { TProduct } from "@/types/product";

type ProductCardProps = {
  product: TProduct;
  shopId: string;
  shopName: string;
};

export function ProductCard({ product, shopId, shopName }: ProductCardProps) {
  const [quantity, setQuantity] = React.useState(1);
  const [isAdded, setIsAdded] = React.useState(false);
  const addItem = useCartStore((state) => state.addItem);

  const isOutOfStock = !product.inStock || product.stockQuantity <= 0;

  const handleIncrement = () => {
    if (quantity < product.stockQuantity) {
      setQuantity((prev) => prev + 1);
    }
  };

  const handleDecrement = () => {
    if (quantity > 1) {
      setQuantity((prev) => prev - 1);
    }
  };

  const handleAddToCart = () => {
    if (isOutOfStock) return;
    const success = addItem(shopId, shopName, product, quantity);
    if (success) {
      setIsAdded(true);
      setTimeout(() => setIsAdded(false), 1500);
      setQuantity(1);
    }
  };

  return (
    <Card
      className={`group relative flex flex-col justify-between overflow-hidden border-border/80 bg-card/60 transition-all duration-300 ${
        isOutOfStock
          ? "opacity-60 grayscale-[40%]"
          : "hover:border-indigo-500/40 hover:shadow-xl hover:shadow-indigo-500/10 hover:-translate-y-1"
      }`}
    >
      {/* Product Image Header */}
      <div className="relative aspect-video w-full overflow-hidden bg-secondary/60 flex items-center justify-center border-b border-border/50">
        {product.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={product.imageUrl}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <div className="flex flex-col items-center justify-center gap-2 text-muted-foreground">
            <Package className="h-10 w-10 stroke-1 text-muted-foreground/60" />
            <span className="text-[11px] font-medium uppercase tracking-wider">Product Visual</span>
          </div>
        )}

        <div className="absolute top-3 right-3">
          <StockBadge
            stockQuantity={product.stockQuantity}
            inStock={product.inStock}
          />
        </div>
      </div>

      <CardHeader className="p-5 pb-3">
        <div className="flex items-baseline justify-between gap-2">
          <h3 className="text-base font-bold text-foreground group-hover:text-primary transition-colors line-clamp-1">
            {product.name}
          </h3>
          <span className="text-base font-black text-indigo-400 shrink-0">
            {formatCurrency(product.price)}
          </span>
        </div>
        <p className="line-clamp-2 mt-1.5 text-xs text-muted-foreground leading-relaxed">
          {product.description || "High quality inventory item available for immediate dispatch."}
        </p>
      </CardHeader>

      <CardContent className="p-5 pt-0 mt-auto">
        {!isOutOfStock && (
          <div className="flex items-center justify-between gap-3 pt-3 border-t border-border/40">
            <span className="text-xs text-muted-foreground font-medium">Quantity:</span>
            <div className="flex items-center rounded-lg border border-border/80 bg-secondary/50 p-0.5">
              <button
                type="button"
                onClick={handleDecrement}
                disabled={quantity <= 1}
                className="flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-card hover:text-foreground disabled:opacity-30 transition-colors"
                aria-label="Decrease quantity"
              >
                <Minus className="h-3.5 w-3.5" />
              </button>
              <span className="w-8 text-center text-xs font-bold text-foreground">
                {quantity}
              </span>
              <button
                type="button"
                onClick={handleIncrement}
                disabled={quantity >= product.stockQuantity}
                className="flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-card hover:text-foreground disabled:opacity-30 transition-colors"
                aria-label="Increase quantity"
              >
                <Plus className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        )}
      </CardContent>

      <CardFooter className="p-5 pt-0">
        <Button
          onClick={handleAddToCart}
          disabled={isOutOfStock}
          variant={isOutOfStock ? "outline" : isAdded ? "default" : "default"}
          className={`w-full font-semibold transition-all ${
            isAdded ? "bg-emerald-600 hover:bg-emerald-700 text-white" : ""
          }`}
        >
          {isOutOfStock ? (
            <span>Sold Out</span>
          ) : isAdded ? (
            <>
              <Check className="h-4 w-4" />
              Added to Cart
            </>
          ) : (
            <>
              <ShoppingCart className="h-4 w-4" />
              Add to Cart
            </>
          )}
        </Button>
      </CardFooter>
    </Card>
  );
}

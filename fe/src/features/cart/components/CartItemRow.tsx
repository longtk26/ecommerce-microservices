import * as React from "react";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/lib/utils";
import { Plus, Minus, Trash2, Package } from "lucide-react";
import type { TCartItem } from "@/types/cart";

type CartItemRowProps = {
  item: TCartItem;
  onUpdateQuantity: (productId: string, quantity: number) => void;
  onRemove: (productId: string) => void;
};

export function CartItemRow({ item, onUpdateQuantity, onRemove }: CartItemRowProps) {
  return (
    <div className="flex items-center gap-3.5 rounded-xl border border-border/60 bg-card/40 p-3.5 transition-colors hover:border-border">
      {/* Thumbnail */}
      <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-secondary/60 flex items-center justify-center border border-border/40">
        {item.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={item.imageUrl}
            alt={item.productName}
            className="h-full w-full object-cover"
          />
        ) : (
          <Package className="h-6 w-6 text-muted-foreground/60" />
        )}
      </div>

      {/* Details */}
      <div className="flex flex-1 flex-col justify-between overflow-hidden">
        <div className="flex items-start justify-between gap-2">
          <h4 className="text-xs font-bold text-foreground line-clamp-1">
            {item.productName}
          </h4>
          <button
            type="button"
            onClick={() => onRemove(item.productId)}
            className="text-muted-foreground hover:text-destructive transition-colors p-1"
            aria-label={`Remove ${item.productName}`}
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="text-[11px] text-muted-foreground">
          {formatCurrency(item.price)} each
        </div>

        <div className="mt-2 flex items-center justify-between">
          {/* Stepper */}
          <div className="flex items-center rounded-md border border-border/80 bg-secondary/50 p-0.5">
            <button
              type="button"
              onClick={() => onUpdateQuantity(item.productId, item.quantity - 1)}
              className="flex h-6 w-6 items-center justify-center rounded text-muted-foreground hover:bg-card hover:text-foreground transition-colors"
              aria-label="Decrease quantity"
            >
              <Minus className="h-3 w-3" />
            </button>
            <span className="w-6 text-center text-xs font-bold text-foreground">
              {item.quantity}
            </span>
            <button
              type="button"
              onClick={() => onUpdateQuantity(item.productId, item.quantity + 1)}
              disabled={item.quantity >= item.stockQuantity}
              className="flex h-6 w-6 items-center justify-center rounded text-muted-foreground hover:bg-card hover:text-foreground disabled:opacity-30 transition-colors"
              aria-label="Increase quantity"
            >
              <Plus className="h-3 w-3" />
            </button>
          </div>

          {/* Line Subtotal */}
          <span className="text-xs font-bold text-foreground">
            {formatCurrency(item.price * item.quantity)}
          </span>
        </div>
      </div>
    </div>
  );
}

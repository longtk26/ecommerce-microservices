import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/utils";
import { ShoppingBag, Store, Package } from "lucide-react";
import type { TCartItem } from "@/types/cart";

type CheckoutSummaryCardProps = {
  items: TCartItem[];
  shopName: string | null;
};

export function CheckoutSummaryCard({ items, shopName }: CheckoutSummaryCardProps) {
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const total = subtotal;

  return (
    <Card className="border-border/80 bg-card/60 backdrop-blur-xl">
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShoppingBag className="h-5 w-5 text-primary" />
            <CardTitle className="text-lg">Order Summary</CardTitle>
          </div>
          {shopName && (
            <div className="flex items-center gap-1 text-xs text-indigo-300 font-medium">
              <Store className="h-3.5 w-3.5" />
              <span>{shopName}</span>
            </div>
          )}
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Item List */}
        <div className="max-h-72 overflow-y-auto space-y-2.5 pr-1">
          {items.map((item) => (
            <div
              key={item.productId}
              className="flex items-center justify-between gap-3 rounded-lg border border-border/50 bg-secondary/30 p-2.5 text-xs"
            >
              <div className="flex items-center gap-2.5 overflow-hidden">
                <div className="h-10 w-10 shrink-0 rounded bg-secondary/80 flex items-center justify-center border border-border/40 overflow-hidden">
                  {item.imageUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={item.imageUrl}
                      alt={item.productName}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <Package className="h-4 w-4 text-muted-foreground/60" />
                  )}
                </div>
                <div className="overflow-hidden">
                  <p className="font-semibold text-foreground truncate">{item.productName}</p>
                  <p className="text-[11px] text-muted-foreground">Qty: {item.quantity} × {formatCurrency(item.price)}</p>
                </div>
              </div>

              <span className="font-bold text-foreground shrink-0">
                {formatCurrency(item.price * item.quantity)}
              </span>
            </div>
          ))}
        </div>

        {/* Pricing calculations */}
        <div className="border-t border-border/60 pt-3 space-y-2 text-xs">
          <div className="flex justify-between text-muted-foreground">
            <span>Subtotal</span>
            <span>{formatCurrency(subtotal)}</span>
          </div>
          <div className="flex justify-between text-muted-foreground">
            <span>Delivery & Handling</span>
            <span className="text-emerald-400 font-medium">Free</span>
          </div>
          <div className="border-t border-border/60 pt-2 flex justify-between text-base font-bold text-foreground">
            <span>Total Amount</span>
            <span className="font-black text-indigo-400">{formatCurrency(total)}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}


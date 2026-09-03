"use client";

import { useEffect } from "react";
import Link from "next/link";
import confetti from "canvas-confetti";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/utils";
import { CheckCircle2, Store, Package, Sparkles } from "lucide-react";
import type { TGetOrderResponse } from "@/types/order";

type OrderCelebrationCardProps = {
  order: TGetOrderResponse;
};

export function OrderCelebrationCard({ order }: OrderCelebrationCardProps) {
  useEffect(() => {
    // Trigger confetti cannon on success
    try {
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
        colors: ["#6366f1", "#a855f7", "#ec4899", "#22c55e"],
      });
    } catch {
      // ignore
    }
  }, []);

  return (
    <Card className="overflow-hidden border-emerald-500/30 bg-card/80 backdrop-blur-xl shadow-2xl shadow-emerald-500/10 animate-in zoom-in-95 duration-300">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-emerald-500/20 via-emerald-600/10 to-teal-500/20 p-8 text-center border-b border-emerald-500/20">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500 text-white shadow-xl shadow-emerald-500/30 mb-4 animate-bounce">
          <CheckCircle2 className="h-9 w-9" />
        </div>
        <Badge variant="success" className="mb-2">
          <Sparkles className="h-3 w-3" />
          Payment Confirmed
        </Badge>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
          Order Successfully Placed!
        </h2>
        <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-md mx-auto">
          Your payment was processed, stock reserved, and order is confirmed.
        </p>
      </div>

      <CardContent className="p-6 sm:p-8 space-y-6">
        {/* Order Details Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 rounded-xl border border-border/60 bg-secondary/30 p-4 text-xs">
          <div>
            <span className="text-muted-foreground block">Order ID</span>
            <span className="font-mono font-bold text-foreground truncate block mt-0.5" title={order.orderId}>
              {order.orderId.slice(0, 13)}...
            </span>
          </div>
          <div>
            <span className="text-muted-foreground block">Customer</span>
            <span className="font-semibold text-foreground block mt-0.5">
              {order.userId}
            </span>
          </div>
          <div>
            <span className="text-muted-foreground block">Total Amount</span>
            <span className="font-black text-emerald-400 text-sm block mt-0.5">
              {formatCurrency(order.totalAmount)}
            </span>
          </div>
        </div>

        {/* Itemized list */}
        <div className="space-y-3">
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Purchased Items ({order.items.length})
          </h4>
          <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
            {order.items.map((item, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between rounded-xl border border-border/50 bg-secondary/40 p-3 text-xs"
              >
                <div className="flex items-center gap-2.5">
                  <div className="h-8 w-8 rounded-lg bg-secondary flex items-center justify-center text-muted-foreground border border-border/40">
                    <Package className="h-4 w-4" />
                  </div>
                  <div>
                    <span className="font-semibold text-foreground block">
                      {item.productName}
                    </span>
                    <span className="text-[11px] text-muted-foreground">
                      Qty: {item.quantity} × {formatCurrency(item.unitPrice)}
                    </span>
                  </div>
                </div>
                <span className="font-bold text-foreground">
                  {formatCurrency(item.unitPrice * item.quantity)}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Action button */}
        <div className="pt-2">
          <Link href="/">
            <Button variant="glow" size="lg" className="w-full gap-2">
              <Store className="h-4 w-4" />
              Continue Shopping
            </Button>
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}

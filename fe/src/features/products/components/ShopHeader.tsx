import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft, Store, ShieldCheck, Sparkles } from "lucide-react";
import type { TShop } from "@/types/shop";

type ShopHeaderProps = {
  shop?: TShop;
  totalProducts?: number;
};

export function ShopHeader({ shop, totalProducts = 0 }: ShopHeaderProps) {
  return (
    <div className="relative overflow-hidden rounded-3xl border border-border/80 bg-card/60 p-6 sm:p-8 backdrop-blur-xl shadow-lg">
      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="space-y-3">
          <Link href="/">
            <Button
              variant="ghost"
              size="sm"
              className="gap-2 text-muted-foreground hover:text-foreground -ml-2 mb-1"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to All Shops
            </Button>
          </Link>

          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500/20 to-purple-500/20 border border-indigo-500/30 text-indigo-400">
              {shop?.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={shop.logoUrl}
                  alt={shop.name}
                  className="h-10 w-10 object-contain rounded-xl"
                />
              ) : (
                <Store className="h-7 w-7" />
              )}
            </div>

            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
                  {shop?.name || "Shop Catalog"}
                </h1>
                <Badge variant="glow" className="text-[11px]">
                  <Sparkles className="h-3 w-3" />
                  Verified
                </Badge>
              </div>
              <p className="mt-1 text-xs sm:text-sm text-muted-foreground max-w-xl">
                {shop?.description || "Browse product collections and available items."}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 self-stretch md:self-auto justify-between md:justify-end border-t md:border-t-0 border-border/50 pt-4 md:pt-0">
          <div className="flex items-center gap-2 rounded-xl border border-border/80 bg-secondary/50 px-4 py-2 text-xs">
            <ShieldCheck className="h-4 w-4 text-emerald-400" />
            <span className="text-muted-foreground">
              Catalog: <strong className="text-foreground">{totalProducts}</strong> products
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}


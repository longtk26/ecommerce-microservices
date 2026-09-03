import { Badge } from "@/components/ui/badge";
import { Sparkles, ShieldCheck, ShoppingBag } from "lucide-react";

export function ShopHero() {
  return (
    <div className="relative overflow-hidden rounded-3xl border border-border/80 bg-gradient-to-b from-card/80 via-card/50 to-background/30 p-8 sm:p-12 backdrop-blur-xl shadow-2xl">
      {/* Background ambient orbs */}
      <div className="pointer-events-none absolute -top-24 left-1/4 h-72 w-72 rounded-full bg-indigo-600/15 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-24 right-1/4 h-72 w-72 rounded-full bg-purple-600/15 blur-3xl" />

      <div className="relative z-10 max-w-3xl space-y-6">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="glow" className="px-3 py-1 text-xs">
            <Sparkles className="h-3.5 w-3.5 text-indigo-400" />
            <span>Curated Marketplace</span>
          </Badge>
          <Badge variant="success" className="px-3 py-1 text-xs">
            <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
            <span>Verified Merchants</span>
          </Badge>
        </div>

        <div className="space-y-3">
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-foreground leading-[1.15]">
            Discover Unique Shops & <br />
            <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              Curated Collections
            </span>
          </h1>
          <p className="text-sm sm:text-base text-muted-foreground leading-relaxed max-w-2xl">
            Select a verified merchant below to explore live product collections, enjoy secure checkout, and track your orders in real time.
          </p>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-2 border-t border-border/50 text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Real-Time Inventory</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 rounded-full bg-indigo-400 animate-pulse" />
            <span>Secure Payments</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 rounded-full bg-purple-400 animate-pulse" />
            <span>Live Order Tracking</span>
          </div>
        </div>
      </div>
    </div>
  );
}


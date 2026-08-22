import { Container } from "@/components/layout/Container";
import { Layers, ShieldCheck, Zap, ArrowRightLeft } from "lucide-react";

export function Footer() {
  return (
    <footer className="mt-auto border-t border-border/60 bg-card/30 backdrop-blur-md py-12 text-sm text-muted-foreground">
      <Container size="xl">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div className="space-y-3 md:col-span-2">
            <div className="flex items-center gap-2 text-foreground font-semibold text-base">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-500/20 text-indigo-400">
                <Layers className="h-4 w-4" />
              </span>
              ShopSaga Microservices Platform
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed max-w-md">
              Powered by Spring Boot microservices and Next.js. Utilizing asynchronous saga choreography
              over RabbitMQ to guarantee eventual consistency across Orders, Inventory reservations, and Payment processing.
            </p>
          </div>

          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-foreground">
              Architecture
            </h4>
            <ul className="space-y-1.5 text-xs">
              <li className="flex items-center gap-1.5">
                <Zap className="h-3.5 w-3.5 text-amber-400" />
                <span>Inventory Service (Port 8082)</span>
              </li>
              <li className="flex items-center gap-1.5">
                <Zap className="h-3.5 w-3.5 text-indigo-400" />
                <span>Order Service (Port 8081)</span>
              </li>
              <li className="flex items-center gap-1.5">
                <Zap className="h-3.5 w-3.5 text-emerald-400" />
                <span>Payment Service (Port 8083)</span>
              </li>
            </ul>
          </div>

          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-foreground">
              Saga Flow
            </h4>
            <div className="space-y-1 text-xs text-muted-foreground">
              <p className="flex items-center gap-1.5">
                <ArrowRightLeft className="h-3.5 w-3.5 text-primary" />
                <span>Choreographed Events</span>
              </p>
              <p className="flex items-center gap-1.5">
                <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
                <span>Compensating Transactions</span>
              </p>
            </div>
          </div>
        </div>

        <div className="border-t border-border/40 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
          <p>© {new Date().getFullYear()} ShopSaga. Built for distributed transaction resilience.</p>
          <p className="text-muted-foreground">Next.js App Router • TanStack Query • Zustand</p>
        </div>
      </Container>
    </footer>
  );
}

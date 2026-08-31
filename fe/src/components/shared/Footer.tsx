import { Container } from "@/components/layout/Container";
import { Layers, ShieldCheck, Zap, ArrowRightLeft, Lock, Compass } from "lucide-react";

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
              Powered by Spring Boot microservices, Spring Cloud Gateway, and Next.js. Secured at the edge with
              AWS Cognito OAuth2 JWT validation and dynamic Eureka service discovery.
            </p>
          </div>

          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-foreground">
              Gateway & Security
            </h4>
            <ul className="space-y-1.5 text-xs">
              <li className="flex items-center gap-1.5">
                <Lock className="h-3.5 w-3.5 text-emerald-400" />
                <span>API Gateway & Cognito (Port 8080)</span>
              </li>
              <li className="flex items-center gap-1.5">
                <Compass className="h-3.5 w-3.5 text-indigo-400" />
                <span>Eureka Discovery (Port 8761)</span>
              </li>
              <li className="flex items-center gap-1.5">
                <Zap className="h-3.5 w-3.5 text-amber-400" />
                <span>Internal Microservices (8081-8083)</span>
              </li>
            </ul>
          </div>

          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-foreground">
              Saga Flow & Auth
            </h4>
            <div className="space-y-1 text-xs text-muted-foreground">
              <p className="flex items-center gap-1.5">
                <ArrowRightLeft className="h-3.5 w-3.5 text-primary" />
                <span>Choreographed Events</span>
              </p>
              <p className="flex items-center gap-1.5">
                <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
                <span>Role-Based AuthZ (RBAC)</span>
              </p>
            </div>
          </div>
        </div>

        <div className="border-t border-border/40 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
          <p>© {new Date().getFullYear()} ShopSaga. Built for distributed transaction resilience.</p>
          <p className="text-muted-foreground">Next.js App Router • Spring Cloud Gateway • TanStack Query • Zustand</p>
        </div>
      </Container>
    </footer>
  );
}

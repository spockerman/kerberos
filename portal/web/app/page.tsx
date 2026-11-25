import { readConfig } from "@/lib/config";
import { PortalApp } from "@/components/PortalApp";

// Configuração lida a cada requisição (variáveis de ambiente do container), não no build.
export const dynamic = "force-dynamic";

export default function Page() {
    return <PortalApp config={readConfig()} />;
}

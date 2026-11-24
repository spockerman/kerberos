import { readConfig } from "@/lib/config";
import { DemoApp } from "@/components/DemoApp";

// A configuração é lida a cada requisição (variáveis de ambiente do container), não no build.
export const dynamic = "force-dynamic";

export default function Page() {
    return <DemoApp config={readConfig()} />;
}

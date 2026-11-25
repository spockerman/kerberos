// Identidade do sistema. Única fonte: design-system/brand/. Trocar a marca não exige mudar este código.
import brandJson from "../../design-system/brand/brand.json";
import logoUrl from "../../design-system/brand/logo.svg";
import logoMarkUrl from "../../design-system/brand/logo-mark.svg";
import appIconUrl from "../../design-system/brand/app-icon.svg";

export const brand = {
    productName: brandJson.productName,
    isPlaceholder: brandJson.placeholder === true,
    /** Logo completo — só em tamanho grande (mín. 48 px de altura) */
    logoUrl,
    /** Marca compacta — cabeçalhos e espaços pequenos */
    logoMarkUrl,
    /** Ícone quadrado — favicon */
    appIconUrl
};

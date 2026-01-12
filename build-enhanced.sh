#!/bin/bash
# Script mejorado de compilacion para Apocalipsis Plugin
# Incluye diagnosticos y troubleshooting

set -e

echo ""
echo "========================================"
echo "  APOCALIPSIS - Compilacion Maven"
echo "  Con Diagnosticos Mejorados"
echo "========================================"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar si Maven esta instalado
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}[ERROR]${NC} Maven no encontrado en PATH"
    echo "Por favor instala Maven: https://maven.apache.org/download.cgi"
    exit 1
fi

# Verificar version de Java
echo -e "${GREEN}[INFO]${NC} Verificando version de Java..."
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "21" ]; then
    echo -e "${YELLOW}[ADVERTENCIA]${NC} Este proyecto requiere Java 21"
    echo "Version actual de Java:"
    java -version
    echo ""
    echo "Descarga Java 21 desde: https://adoptium.net/temurin/releases/?version=21"
    echo ""
    read -p "Deseas continuar de todos modos? (s/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        exit 1
    fi
fi

# Verificar conectividad con repositorio Paper MC
echo -e "${GREEN}[INFO]${NC} Verificando conectividad con Paper MC repository..."
if ! ping -c 1 repo.papermc.io &> /dev/null; then
    echo -e "${YELLOW}[ADVERTENCIA]${NC} No se puede acceder a repo.papermc.io"
    echo "Esto puede causar problemas al descargar dependencias"
    echo ""
    echo "Soluciones:"
    echo "1. Verifica tu conexion a Internet"
    echo "2. Verifica configuracion de firewall/proxy"
    echo "3. Consulta TROUBLESHOOTING_APOCALIPSIS_CLASS.md"
    echo ""
    read -p "Deseas continuar? (s/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}[1/4]${NC} Limpiando build anterior y cache..."
if ! mvn clean; then
    echo -e "${RED}[ERROR]${NC} Fallo al limpiar"
    echo ""
    echo "Intenta ejecutar manualmente:"
    echo "  mvn clean -X"
    echo ""
    exit 1
fi

echo ""
echo -e "${GREEN}[2/4]${NC} Actualizando dependencias (esto puede tomar tiempo)..."
if ! mvn dependency:resolve -U; then
    echo -e "${RED}[ERROR]${NC} Fallo al resolver dependencias"
    echo ""
    echo "Posibles causas:"
    echo "- Paper API no disponible"
    echo "- Problemas de red"
    echo "- Cache de Maven corrupto"
    echo ""
    echo "Soluciones:"
    echo "1. Ejecuta: mvn clean install -U"
    echo "2. Borra cache: rm -rf ~/.m2/repository/io/papermc"
    echo "3. Consulta TROUBLESHOOTING_APOCALIPSIS_CLASS.md"
    echo ""
    exit 1
fi

echo ""
echo -e "${GREEN}[3/4]${NC} Compilando plugin..."
if ! mvn compile; then
    echo -e "${RED}[ERROR]${NC} Fallo la compilacion"
    echo ""
    echo "Si ves 'cannot access Apocalipsis':"
    echo "1. Limpia el proyecto: mvn clean"
    echo "2. Actualiza en tu IDE: Reload Maven Project"
    echo "3. Consulta TROUBLESHOOTING_APOCALIPSIS_CLASS.md"
    echo ""
    exit 1
fi

echo ""
echo -e "${GREEN}[4/4]${NC} Empaquetando JAR final..."
if ! mvn package -DskipTests; then
    echo -e "${RED}[ERROR]${NC} Fallo al empaquetar"
    exit 1
fi

echo ""
echo -e "${GREEN}[INFO]${NC} Verificando archivo JAR..."
if [ -f "target/Apocalipsis-1.0.1.jar" ]; then
    echo ""
    echo "========================================"
    echo -e "  ${GREEN}✓ COMPILACION EXITOSA!${NC}"
    echo "========================================"
    echo ""
    echo "Archivo generado: target/Apocalipsis-1.0.1.jar"
    JAR_SIZE=$(stat -f%z "target/Apocalipsis-1.0.1.jar" 2>/dev/null || stat -c%s "target/Apocalipsis-1.0.1.jar" 2>/dev/null)
    echo "Tamano: $JAR_SIZE bytes"
    echo ""
    echo "Instrucciones:"
    echo "1. Copia el JAR a la carpeta plugins/ de tu servidor Paper"
    echo "2. Reinicia el servidor"
    echo "3. Los archivos de config se generaran automaticamente"
    echo ""
    echo "Archivos de configuracion que se generan:"
    echo "- config.yml"
    echo "- desastres.yml"
    echo "- eventos.yml"
    echo "- misiones_new.yml"
    echo "- rangos.yml"
    echo "- recompensas.yml"
    echo ""
else
    echo -e "${RED}[ERROR]${NC} No se genero el archivo JAR esperado"
    echo ""
    echo "Verifica:"
    echo "1. Que la compilacion termino sin errores"
    echo "2. Que target/ tiene permisos de escritura"
    echo "3. Los logs de Maven arriba para mas detalles"
    echo ""
    exit 1
fi

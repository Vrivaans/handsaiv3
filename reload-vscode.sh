#!/bin/bash

echo "🔄 Forzando recarga completa de VS Code con Lombok..."

# Matar todos los procesos de VS Code
pkill -f "Visual Studio Code" 2>/dev/null || true
pkill -f "redhat.java" 2>/dev/null || true

# Limpiar cachés
rm -rf .vscode/.java-* 2>/dev/null || true
rm -rf .metadata 2>/dev/null || true
rm -rf target/classes 2>/dev/null || true

# Compilar con Maven para asegurar que Lombok funciona
echo "📦 Compilando proyecto con Maven..."
JAVA_HOME=/Users/ivanv/.sdkman/candidates/java/21.0.4-oracle ./mvnw clean compile

echo "✅ Listo! Ahora abre VS Code con: code ."
echo "⚠️  IMPORTANTE: Espera 2-3 minutos a que cargue completamente antes de verificar los errores."
# Extrai os zips da base (baixados do link do edital) para a pasta data/ do repositorio.
# Uso: ./scripts/preparar-dados.ps1 [-Origem C:\caminho\dos\zips]
param(
    [string]$Origem = (Join-Path $env:USERPROFILE 'Downloads')
)

$ErrorActionPreference = 'Stop'
$destino = Join-Path $PSScriptRoot '..\data'
New-Item -ItemType Directory -Force $destino | Out-Null

$nomes = 'Rush Series Esquenta-*', 'Rush Series Esquenta 2-*', 'Rush Series Inclusivo-*', 'Univav*-*'
$zips = foreach ($n in $nomes) { Get-ChildItem -Path $Origem -Filter "$n.zip" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 }

if (-not $zips) {
    Write-Error "Nenhum zip da base encontrado em $Origem. Baixe pelo link do edital ou informe -Origem."
}

foreach ($z in $zips) {
    Write-Host "Extraindo $($z.Name)..."
    Expand-Archive -Path $z.FullName -DestinationPath $destino -Force
}

$total = (Get-ChildItem -Path $destino -Recurse -Filter *.json).Count
Write-Host "Pronto: $total arquivos JSON em $(Resolve-Path $destino) (esperado: 336)."

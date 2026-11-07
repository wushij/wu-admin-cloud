$ErrorActionPreference = 'Stop'
Set-Location 'e:\wu-admin-cloud'

# ---------- backend endpoints ----------
$backend = New-Object System.Collections.Generic.List[string]
$srcRoots = @('wu-cloud-service','wu-cloud-auth','wu-admin','wu-cloud-infra','wu-cloud-api','wu-cloud-common')
$javaFiles = foreach($r in $srcRoots){
  if(Test-Path $r){ Get-ChildItem -Path $r -Recurse -Filter '*.java' -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\target\\' -and $_.FullName -notmatch '\\src\\test\\' } }
}
foreach($f in $javaFiles){
  $lines = Get-Content $f.FullName -Encoding UTF8
  $cls = $null; $pending = $null
  foreach($l in $lines){
    $t = $l.Trim()
    if($t -match '^(?:@\w+\s+)*(?:public\s+|final\s+|abstract\s+|static\s+)*(class|record|interface|enum)\s+\w+'){
      if($pending){ $cls = $pending } else { $cls = $null }
      $pending = $null; continue
    }
    if($t -match '^@RequestMapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"\s*\))?'){
      $p = $Matches[1]; if($null -eq $p){ $p = '' }
      $pending = $p; continue
    }
    if($t -match '^@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)")?\s*\)?'){
      $verb = $Matches[1].ToUpper(); $p = $Matches[2]; if($null -eq $p){ $p = '' }
      $backend.Add("$verb`t$($cls)$p")
    }
  }
}
$norm = @{}
foreach($e in $backend){
  $i = $e.IndexOf("`t"); $v = $e.Substring(0,$i); $p = $e.Substring($i+1)
  $p = ($p -replace '\{[^}]+\}','*') -replace '/+$',''
  if($p -eq ''){ $p = '/' }
  $norm["$v`t$p"] = $true
}
Write-Output ("backend endpoints: {0}" -f $norm.Count)

function Normalize([string]$raw){
  $p = [regex]::Replace($raw, '\$\{[^}]*\}', '*')
  $p = ($p -split '\?')[0]
  $p = $p -replace '/+$',''
  return $p
}

foreach($target in @('wu-admin-uniapp','wu-admin-web')){
  $missing = New-Object System.Collections.Generic.List[object]
  $apiFiles = Get-ChildItem -Path "$target\src\api" -Recurse -Filter '*.ts'
  foreach($f in $apiFiles){
    $lines = Get-Content $f.FullName -Encoding UTF8
    for($i=0;$i -lt $lines.Count;$i++){
      $l = $lines[$i]
      $verb = $null
      if($l -match '\b(get|post|put|del|delete|patch)\s*[<(]'){ $verb = $Matches[1].ToLower() }
      if(-not $verb){ continue }
      $verb = switch($verb){ 'del'{'DELETE'} 'delete'{'DELETE'} 'get'{'GET'} 'post'{'POST'} 'put'{'PUT'} 'patch'{'PATCH'} default{$verb.ToUpper()} }
      $lits = @()
      foreach($mm in [regex]::Matches($l, "'([^']*)'")){ $lits += $mm.Groups[1].Value }
      foreach($mm in [regex]::Matches($l, '"([^"]*)"')){ $lits += $mm.Groups[1].Value }
      foreach($mm in [regex]::Matches($l, '``([^``]*)``')){ $lits += $mm.Groups[1].Value }
      foreach($raw in $lits){
        if(-not $raw.StartsWith('/')){ continue }
        if($raw -eq '/api' -or $raw -eq '/api/'){ continue }
        $full = Normalize ('/api' + $raw)
        $key = "$verb`t$full"
        if(-not $norm.ContainsKey($key)){
          $missing.Add([pscustomobject]@{ Verb=$verb; Path=('/api'+$raw); Normalized=$full; File=$f.Name; Line=$i+1 })
        }
      }
    }
  }
  Write-Output ""
  Write-Output "=== MISSING [$target] ==="
  $u = $missing | Sort-Object Verb,Normalized -Unique
  Write-Output ("count: {0}" -f $u.Count)
  $u | ForEach-Object { "{0,-7} {1}   <- {2}:{3}" -f $_.Verb, $_.Normalized, $_.File, $_.Line }
}

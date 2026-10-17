param(
  [string]$BaseUrl = "http://localhost:8080",
  [int]$Users = 10
)

$ErrorActionPreference = "Stop"

$requests = @(
  @{ Name = "map stats"; Url = "$BaseUrl/api/map/stats" },
  @{ Name = "area recommend"; Url = "$BaseUrl/api/map/recommend-areas?type=all&sort=hot&limit=10" },
  @{ Name = "areas"; Url = "$BaseUrl/api/map/areas?limit=20" },
  @{ Name = "diary stats"; Url = "$BaseUrl/api/diaries/stats" },
  @{ Name = "diary fulltext"; Url = "$BaseUrl/api/diaries/fulltext?keyword=%E6%88%90%E9%83%BD&limit=10" }
)

$jobs = 1..$Users | ForEach-Object {
  $userNo = $_
  Start-Job -ArgumentList $userNo, $requests -ScriptBlock {
    param($userNo, $requests)
    $results = @()
    foreach ($request in $requests) {
      $watch = [System.Diagnostics.Stopwatch]::StartNew()
      try {
        Invoke-RestMethod -Uri $request.Url -Method Get | Out-Null
        $watch.Stop()
        $results += [pscustomobject]@{
          User = $userNo
          Name = $request.Name
          Status = "OK"
          Milliseconds = $watch.ElapsedMilliseconds
        }
      } catch {
        $watch.Stop()
        $results += [pscustomobject]@{
          User = $userNo
          Name = $request.Name
          Status = "FAIL"
          Milliseconds = $watch.ElapsedMilliseconds
        }
      }
    }
    $results
  }
}

$rows = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$rows | Sort-Object User, Name | Format-Table -AutoSize
$ok = ($rows | Where-Object { $_.Status -eq "OK" }).Count
$total = $rows.Count
$avg = [math]::Round(($rows | Measure-Object -Property Milliseconds -Average).Average, 2)
Write-Host "Summary: $ok / $total requests OK, average ${avg}ms, concurrent users: $Users"

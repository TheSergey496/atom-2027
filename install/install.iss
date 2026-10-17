; ============================================================================
;  Инсталлятор It Just Works — Inno Setup 6+
; ============================================================================

#define MyAppName        "It Just Works"
#define MyAppVersion     "1.0"
#define MyAppPublisher   "Pro Legacy"
#define MyAppURL         "https://example.com"

[Setup]
AppId={{8BDB29F1-F946-423A-9F37-529591CDA764}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
PrivilegesRequired=lowest
OutputBaseFilename=install
SolidCompression=yes
WizardStyle=modern dynamic

DefaultDirName={src}\{#MyAppName}
DisableDirPage=yes
UsePreviousAppDir=no
AllowNoIcons=yes

Uninstallable=yes
CreateUninstallRegKey=yes

[Languages]
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"

[Files]
Source: "images.tar";         DestDir: "{app}"; Flags: ignoreversion
Source: "docker-compose.yml"; DestDir: "{app}"; Flags: ignoreversion
Source: "install-docker.ps1"; DestDir: "{app}"; Flags: ignoreversion

Source: "images\*.png";  Flags: dontcopy

[UninstallRun]
Filename: "powershell.exe"; \
  Parameters: "-NoProfile -ExecutionPolicy Bypass -Command ""Set-Location -LiteralPath '{app}'; docker compose down -v --remove-orphans 2>&1 | Out-Null"""; \
  Flags: runhidden; RunOnceId: "ComposeDown"

[UninstallDelete]
Type: files; Name: "{app}\install.log"
Type: files; Name: "{app}\install.exit"
Type: files; Name: "{app}\install.pid"
Type: files; Name: "{app}\docker-compose.out.log"
Type: files; Name: "{app}\docker-compose.err.log"

[Code]
var
  InfoPage:         TOutputMsgWizardPage;
  AccessPage:       TInputQueryWizardPage;
  AdvancedCheck:    TCheckBox;
  ProductsPage:     TOutputMsgWizardPage;
  InstallSucceeded: Boolean;

  DeployVerbs: array[0..19] of String;
  DeployAdjs:  array[0..19] of String;
  DeployNouns: array[0..23] of String;
  DeployTails: array[0..19] of String;
  DeploySeed:       Cardinal;

  CarouselImage:  TBitmapImage;
  CarouselFiles:  TArrayOfString;
  CarouselIndex:  Integer;
  CarouselLoaded: Boolean;
  
  FinishedLinks: TLinkLabel;

const
  PM_REMOVE = 1;

type
  TMsgBuffer = array[0..7] of Int64;

function PeekMessageW(var lpMsg: TMsgBuffer; hWnd: HWND;
                      wMsgFilterMin, wMsgFilterMax, wRemoveMsg: Cardinal): BOOL;
  external 'PeekMessageW@user32.dll stdcall';
function TranslateMessage(var lpMsg: TMsgBuffer): BOOL;
  external 'TranslateMessage@user32.dll stdcall';
function DispatchMessageW(var lpMsg: TMsgBuffer): Longint;
  external 'DispatchMessageW@user32.dll stdcall';
function GetTickCount: DWord;
  external 'GetTickCount@kernel32.dll stdcall';

procedure PumpMessages;
var
  Msg: TMsgBuffer;
begin
  while PeekMessageW(Msg, 0, 0, 0, PM_REMOVE) do
  begin
    TranslateMessage(Msg);
    DispatchMessageW(Msg);
  end;
end;

procedure SleepResponsive(Ms: Integer);
begin
  while Ms > 0 do
  begin
    if Ms > 25 then
    begin
      Sleep(25);
      Ms := Ms - 25;
    end
    else
    begin
      Sleep(Ms);
      Ms := 0;
    end;
    PumpMessages;
  end;
end;

procedure AnimateProgressTo(TargetPosition, DurationMs: Integer);
var
  StartPosition, Distance, Steps, StepMs, I: Integer;
  Numerator, Denominator, NewPosition: Integer;
begin
  StartPosition := WizardForm.ProgressGauge.Position;

  if StartPosition >= TargetPosition then
  begin
    WizardForm.ProgressGauge.Position := TargetPosition;
    WizardForm.ProgressGauge.Update;
    Exit;
  end;

  Distance := TargetPosition - StartPosition;

  if DurationMs < 50  then DurationMs := 50;
  if DurationMs > 1500 then DurationMs := 1500;

  StepMs := 16;
  Steps  := DurationMs div StepMs;
  if Steps < 1 then Steps := 1;

  Denominator := Steps * Steps;

  for I := 1 to Steps do
  begin
    SleepResponsive(StepMs);

    Numerator := 2 * I * Steps - I * I;

    NewPosition := StartPosition + (Distance * Numerator) div Denominator;
    if NewPosition > TargetPosition then
      NewPosition := TargetPosition;

    WizardForm.ProgressGauge.Position := NewPosition;
    WizardForm.ProgressGauge.Update;
  end;

  WizardForm.ProgressGauge.Position := TargetPosition;
  WizardForm.ProgressGauge.Update;
  WizardForm.Repaint;
end;

{ ---------- Карусель ---------- }

function IsImageFile(const Name: String): Boolean;
var
  Ext: String;
begin
  Ext := LowerCase(ExtractFileExt(Name));
  Result := (Ext = '.png') or (Ext = '.jpg') or
            (Ext = '.jpeg') or (Ext = '.bmp');
end;

procedure LayoutCarousel;
var
  TopOffset: Integer;
begin
  if CarouselImage = nil then Exit;
  TopOffset := WizardForm.ProgressGauge.Top +
               WizardForm.ProgressGauge.Height + ScaleY(12);
  CarouselImage.Left   := ScaleX(16);
  CarouselImage.Top    := TopOffset;
  CarouselImage.Width  := WizardForm.InstallingPage.ClientWidth - ScaleX(32);
  if CarouselImage.Width < ScaleX(100) then
    CarouselImage.Width := ScaleX(100);
  CarouselImage.Height := WizardForm.InstallingPage.ClientHeight -
                          TopOffset - ScaleY(12);
  if CarouselImage.Height < ScaleY(60) then
    CarouselImage.Height := ScaleY(60);
end;

procedure InitCarousel;
begin
  CarouselImage := TBitmapImage.Create(WizardForm.InstallingPage);
  CarouselImage.Parent  := WizardForm.InstallingPage;
  CarouselImage.Stretch := True;
  CarouselImage.Visible := False;
  CarouselLoaded := False;
  LayoutCarousel;
end;

procedure LoadCarouselImages;
var
  FindRec: TFindRec;
  Path, TmpName: String;
  I, J: Integer;
begin
  if CarouselLoaded then Exit;
  CarouselLoaded := True;

  ExtractTemporaryFiles('*.png');

  SetArrayLength(CarouselFiles, 0);
  Path := ExpandConstant('{tmp}\');
  if FindFirst(Path + '*.png', FindRec) then
  begin
    try
      repeat
        if IsImageFile(FindRec.Name) then
        begin
          SetArrayLength(CarouselFiles, GetArrayLength(CarouselFiles) + 1);
          CarouselFiles[GetArrayLength(CarouselFiles) - 1] := Path + FindRec.Name;
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  if GetArrayLength(CarouselFiles) > 1 then
    for I := 0 to GetArrayLength(CarouselFiles) - 2 do
      for J := I + 1 to GetArrayLength(CarouselFiles) - 1 do
        if CompareText(CarouselFiles[I], CarouselFiles[J]) > 0 then
        begin
          TmpName := CarouselFiles[I];
          CarouselFiles[I] := CarouselFiles[J];
          CarouselFiles[J] := TmpName;
        end;

  if (CarouselImage <> nil) and (GetArrayLength(CarouselFiles) > 0) then
  begin
    try
      CarouselImage.PngImage.LoadFromFile(CarouselFiles[0]);
      CarouselIndex := 0;
      CarouselImage.Visible := True;
    except
      CarouselImage.Visible := False;
    end;
  end;
end;

procedure NextCarouselImage;
begin
  if (CarouselImage = nil) or (GetArrayLength(CarouselFiles) < 2) then Exit;
  CarouselIndex := (CarouselIndex + 1) mod GetArrayLength(CarouselFiles);
  try
    CarouselImage.PngImage.LoadFromFile(CarouselFiles[CarouselIndex]);
    CarouselImage.Repaint;
  except
  end;
end;

{ ---------- Словари ---------- }

procedure InitWordBanks;
begin
  DeployVerbs[0]  := 'Инициализация';
  DeployVerbs[1]  := 'Оптимизация';
  DeployVerbs[2]  := 'Синхронизация';
  DeployVerbs[3]  := 'Развёртывание';
  DeployVerbs[4]  := 'Калибровка';
  DeployVerbs[5]  := 'Интеграция';
  DeployVerbs[6]  := 'Масштабирование';
  DeployVerbs[7]  := 'Активация';
  DeployVerbs[8]  := 'Реструктуризация';
  DeployVerbs[9]  := 'Кэширование';
  DeployVerbs[10] := 'Балансировка';
  DeployVerbs[11] := 'Оркестрация';
  DeployVerbs[12] := 'Контейнеризация';
  DeployVerbs[13] := 'Виртуализация';
  DeployVerbs[14] := 'Автоматизация';
  DeployVerbs[15] := 'Модернизация';
  DeployVerbs[16] := 'Репликация';
  DeployVerbs[17] := 'Кластеризация';
  DeployVerbs[18] := 'Миграция';
  DeployVerbs[19] := 'Реконфигурация';

  DeployAdjs[0]  := 'облачных';
  DeployAdjs[1]  := 'микросервисных';
  DeployAdjs[2]  := 'нейросетевых';
  DeployAdjs[3]  := 'квантовых';
  DeployAdjs[4]  := 'распределённых';
  DeployAdjs[5]  := 'гибридных';
  DeployAdjs[6]  := 'адаптивных';
  DeployAdjs[7]  := 'отказоустойчивых';
  DeployAdjs[8]  := 'высоконагруженных';
  DeployAdjs[9]  := 'самообучающихся';
  DeployAdjs[10] := 'мультитенантных';
  DeployAdjs[11] := 'событийно-ориентированных';
  DeployAdjs[12] := 'интеллектуальных';
  DeployAdjs[13] := 'сквозных';
  DeployAdjs[14] := 'цифровых';
  DeployAdjs[15] := 'программно-определяемых';
  DeployAdjs[16] := 'масштабируемых';
  DeployAdjs[17] := 'гетерогенных';
  DeployAdjs[18] := 'федеративных';
  DeployAdjs[19] := 'конвергентных';

  DeployNouns[0]  := 'вычислений';
  DeployNouns[1]  := 'архитектур';
  DeployNouns[2]  := 'модулей';
  DeployNouns[3]  := 'алгоритмов';
  DeployNouns[4]  := 'решений';
  DeployNouns[5]  := 'сервисов';
  DeployNouns[6]  := 'платформ';
  DeployNouns[7]  := 'инфраструктур';
  DeployNouns[8]  := 'интерфейсов';
  DeployNouns[9]  := 'протоколов';
  DeployNouns[10] := 'конвейеров';
  DeployNouns[11] := 'кластеров';
  DeployNouns[12] := 'микросервисов';
  DeployNouns[13] := 'процессов';
  DeployNouns[14] := 'потоков';
  DeployNouns[15] := 'транзакций';
  DeployNouns[16] := 'API';
  DeployNouns[17] := 'баз данных';
  DeployNouns[18] := 'сценариев';
  DeployNouns[19] := 'пайплайнов';
  DeployNouns[20] := 'интеграций';
  DeployNouns[21] := 'экосистем';
  DeployNouns[22] := 'приложений';
  DeployNouns[23] := 'нагрузок';

  DeployTails[0]  := 'в реальном времени';
  DeployTails[1]  := 'в рамках единой экосистемы';
  DeployTails[2]  := 'с применением ИИ';
  DeployTails[3]  := 'на основе машинного обучения';
  DeployTails[4]  := 'в облачной среде';
  DeployTails[5]  := 'через REST API';
  DeployTails[6]  := 'с использованием блокчейна';
  DeployTails[7]  := 'для повышения отказоустойчивости';
  DeployTails[8]  := 'в режиме 24/7';
  DeployTails[9]  := 'с нулевым простоем';
  DeployTails[10] := 'по требованию';
  DeployTails[11] := 'под управлением Kubernetes';
  DeployTails[12] := 'для цифровой трансформации';
  DeployTails[13] := 'с горизонтом планирования 5 лет';
  DeployTails[14] := 'по методологии DevOps';
  DeployTails[15] := 'в парадигме Web 3.0';
  DeployTails[16] := 'с учётом лучших практик';
  DeployTails[17] := 'в условиях неопределённости';
  DeployTails[18] := 'с переходом на микросервисы';
  DeployTails[19] := 'в глобальном масштабе';
end;

{ ---------- Ссылки на финальной странице ---------- }

procedure FinishedLinksLinkClick(Sender: TObject; const Link: String;
                                 LinkType: TSysLinkType);
var
  ErrCode: Integer;
begin
  ShellExec('open', Link, '', '', SW_SHOWNORMAL, ewNoWait, ErrCode);
end;

procedure InitFinishedLinks;
begin
  FinishedLinks := TLinkLabel.Create(WizardForm);
  FinishedLinks.Parent      := WizardForm.FinishedPage;
  FinishedLinks.Left        := WizardForm.FinishedLabel.Left;
  FinishedLinks.Top         := WizardForm.FinishedLabel.Top;
  FinishedLinks.Width       := WizardForm.FinishedLabel.Width;
  FinishedLinks.AutoSize    := True;
  FinishedLinks.Visible     := False;
  FinishedLinks.OnLinkClick := @FinishedLinksLinkClick;
end;

{ ---------- Вспомогательные функции ---------- }

function RunAndCapture(const Exe, Params: String; var OutText: String): Integer;
var
  TmpFile, FullCmd: String;
  Lines: TArrayOfString;
  I, ResultCode: Integer;
begin
  TmpFile := ExpandConstant('{tmp}\runout.txt');
  DeleteFile(TmpFile);

  FullCmd := '/C "' + Exe + ' ' + Params + ' > "' + TmpFile + '" 2>&1"';

  if not Exec(ExpandConstant('{cmd}'), FullCmd, '', SW_HIDE,
              ewWaitUntilTerminated, ResultCode) then
  begin
    OutText := 'Не удалось запустить cmd.exe';
    Result  := -1;
    Exit;
  end;

  OutText := '';
  if LoadStringsFromFile(TmpFile, Lines) then
    for I := 0 to GetArrayLength(Lines) - 1 do
      OutText := OutText + Lines[I] + #13#10;

  Result := ResultCode;
end;

function GetTail(const FileName: String; MaxLines: Integer): String;
var
  Lines: TArrayOfString;
  I, Start: Integer;
begin
  Result := '';
  if LoadStringsFromFile(FileName, Lines) then
  begin
    Start := GetArrayLength(Lines) - MaxLines;
    if Start < 0 then Start := 0;
    for I := Start to GetArrayLength(Lines) - 1 do
      Result := Result + Lines[I] + #13#10;
  end;
end;

function CheckDocker(out ErrMsg: String): Boolean;
var
  Output: String;
  Code: Integer;
begin
  Result := False;
  ErrMsg := '';

  Code := RunAndCapture('docker', '--version', Output);
  if Code <> 0 then
  begin
    ErrMsg := 'Docker не обнаружен.' + #13#10 +
              'Установите Docker Desktop с https://www.docker.com/products/docker-desktop ' +
              'и запустите инсталлятор снова.';
    Exit;
  end;

  Code := RunAndCapture('docker', 'info', Output);
  if Code <> 0 then
  begin
    ErrMsg := 'Docker установлен, но демон не запущен.' + #13#10 +
              'Запустите Docker Desktop и повторите установку.' + #13#10#13#10 +
              'Подробности:' + #13#10 + Output;
    Exit;
  end;

  Result := True;
end;

function IsDirWritable(const Dir: String): Boolean;
var
  TestFile: String;
begin
  TestFile := AddBackslash(Dir) + '.write-test.tmp';
  Result := SaveStringToFile(TestFile, 'test', False);
  if Result then
    DeleteFile(TestFile);
end;

function IsValidHostOrIp(const S: String): Boolean;
var
  I: Integer;
  C: Char;
  HasColon: Boolean;
begin
  Result := False;
  if (S = '') or (Length(S) > 253) then Exit;

  HasColon := False;
  for I := 1 to Length(S) do
    if S[I] = ':' then HasColon := True;

  if HasColon then
  begin
    for I := 1 to Length(S) do
    begin
      C := S[I];
      if not (((C >= 'a') and (C <= 'f')) or
              ((C >= 'A') and (C <= 'F')) or
              ((C >= '0') and (C <= '9')) or
              (C = ':') or (C = '%') or
              ((C >= 'a') and (C <= 'z')) or
              ((C >= 'A') and (C <= 'Z'))) then
        Exit;
    end;
  end
  else
  begin
    for I := 1 to Length(S) do
    begin
      C := S[I];
      if not (((C >= 'a') and (C <= 'z')) or
              ((C >= 'A') and (C <= 'Z')) or
              ((C >= '0') and (C <= '9')) or
              (C = '.') or (C = '-') or (C = '_')) then
        Exit;
    end;

    if (S[1] = '.') or (S[1] = '-') or
       (S[Length(S)] = '.') or (S[Length(S)] = '-') then
      Exit;

    if Pos('..', S) > 0 then Exit;
  end;

  Result := True;
end;

function Mix32(X: Cardinal): Cardinal;
begin
  X := X xor (X shr 16);
  X := X * 2146121005;
  X := X xor (X shr 15);
  X := X * 1274126177;
  X := X xor (X shr 16);
  Result := X;
end;

function GetDeployText(Index: Integer): String;
var
  H: Cardinal;
  I1, I2, I3, I4: Integer;
begin
  H := Mix32(Cardinal(Index) + 1 + DeploySeed);

  I1 := H mod 20;  H := H div 20;
  I2 := H mod 20;  H := H div 20;
  I3 := H mod 24;  H := H div 24;
  I4 := H mod 20;

  Result := DeployVerbs[I1] + ' ' +
            DeployAdjs[I2]  + ' ' +
            DeployNouns[I3];

  if (Mix32(Cardinal(Index) + 7919 + DeploySeed) and 3) <> 0 then
    Result := Result + ' ' + DeployTails[I4];
end;

{ ---------- Управление зависшим процессом ---------- }

function ReadPidFile(const FileName: String; var Pid: Cardinal): Boolean;
var
  S: AnsiString;
begin
  Result := False;
  Pid := 0;
  if LoadStringFromFile(FileName, S) then
  begin
    Pid := Cardinal(StrToIntDef(Trim(S), 0));
    Result := Pid <> 0;
  end;
end;

function KillProcessById(Pid: Cardinal): Boolean;
var
  ResultCode: Integer;
begin
  Result := False;
  if not Exec(ExpandConstant('{cmd}'),
              '/C taskkill /F /PID ' + IntToStr(Pid) + ' >NUL 2>&1',
              '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
    Exit;
  Result := (ResultCode = 0);
end;

{ ---------- Логика расширенных настроек ---------- }

procedure AdvancedCheckClick(Sender: TObject);
begin
  AccessPage.PromptLabels[2].Visible := AdvancedCheck.Checked;
  AccessPage.Edits[2].Visible        := AdvancedCheck.Checked;
  AccessPage.PromptLabels[3].Visible := AdvancedCheck.Checked;
  AccessPage.Edits[3].Visible        := AdvancedCheck.Checked;
  AccessPage.PromptLabels[4].Visible := AdvancedCheck.Checked;
  AccessPage.Edits[4].Visible        := AdvancedCheck.Checked;
end;

{ ---------- Развёртывание ---------- }

function RunDeployment(var OutExitCode: Integer): Boolean;
var
  Params, MarkerFile, PidFile: String;
  ExitCodeStr: AnsiString;
  DummyResultCode: Integer;
  Tick, SlicesInCycle, TotalSlices: Integer;
  SliceMs, SlicesPerTick, MaxSlices: Integer;
  PsPid: Cardinal;
begin
  Result := False;
  OutExitCode := 99;

  LoadCarouselImages;

  SliceMs       := 100;
  SlicesPerTick := 12;
  MaxSlices     := 9000;

  MarkerFile := ExpandConstant('{app}\install.exit');
  PidFile    := ExpandConstant('{app}\install.pid');
  DeleteFile(MarkerFile);
  DeleteFile(PidFile);

  Params :=
    '-NoProfile -ExecutionPolicy Bypass ' +
    '-File "' + ExpandConstant('{app}\install-docker.ps1') + '" ' +
    '-InstallDir "' + ExpandConstant('{app}') + '" ' +
    '-BackendHost "' + AccessPage.Values[0] + '" ' +
    '-BackendPort "' + AccessPage.Values[1] + '" ' +
    '-FrontendHost "' + AccessPage.Values[2] + '" ' +
    '-FrontendPort "' + AccessPage.Values[3] + '" ' +
    '-DbPort "' + AccessPage.Values[4] + '"';

  if not Exec('powershell.exe', Params, ExpandConstant('{app}'),
              SW_HIDE, ewNoWait, DummyResultCode) then
  begin
    Exit;
  end;

  WizardForm.ProgressGauge.Style := npbstNormal;
  WizardForm.ProgressGauge.Max := 100;
  WizardForm.ProgressGauge.Position := 0;
  WizardForm.ProgressGauge.Update;
  WizardForm.StatusLabel.Caption := GetDeployText(0);
  WizardForm.StatusLabel.Update;
  WizardForm.Repaint;

  Tick          := 0;
  SlicesInCycle := 0;
  TotalSlices   := 0;

  while not FileExists(MarkerFile) do
  begin
    SleepResponsive(SliceMs);
    Inc(SlicesInCycle);
    Inc(TotalSlices);

    WizardForm.Repaint;

    if SlicesInCycle >= SlicesPerTick then
    begin
      SlicesInCycle := 0;
      Inc(Tick);

      WizardForm.StatusLabel.Caption := GetDeployText(Tick);
      WizardForm.StatusLabel.Update;

      if (Tick mod 3) = 0 then
        NextCarouselImage;

      if WizardForm.ProgressGauge.Position < 95 then
      begin
        WizardForm.ProgressGauge.Position := WizardForm.ProgressGauge.Position + 1;
        WizardForm.ProgressGauge.Update;
      end;
    end;

    if TotalSlices > MaxSlices then
    begin
      if ReadPidFile(PidFile, PsPid) then
      begin
        if KillProcessById(PsPid) then
          Log('Прибит зависший процесс установки (PID ' + IntToStr(PsPid) + ').')
        else
          Log('Не удалось прибить процесс установки (PID ' + IntToStr(PsPid) + ').');
      end;
      OutExitCode := 99;
      Exit;
    end;
  end;

  if not LoadStringFromFile(MarkerFile, ExitCodeStr) then
  begin
    OutExitCode := 99;
    Exit;
  end;

  DeleteFile(MarkerFile);
  DeleteFile(PidFile);

  OutExitCode := StrToIntDef(Trim(ExitCodeStr), 99);
  Result := True;

  if OutExitCode = 0 then
    WizardForm.StatusLabel.Caption := 'Завершение установки...'
  else
    WizardForm.StatusLabel.Caption := 'Завершение с ошибками...';
  WizardForm.StatusLabel.Update;

  AnimateProgressTo(100, 400);
end;

{ ---------- Мастер ---------- }

procedure InitializeWizard();
var
  Y, PortWidth, Gap, HostWidth, PortX: Integer;
begin
  DeploySeed := GetTickCount;
  
  InitWordBanks;
  InitCarousel;
  InitFinishedLinks;

  InfoPage := CreateOutputMsgPage(wpWelcome,
    'Добро пожаловать в {#MyAppName}',
    'Об этом инсталляторе',
    '{#MyAppName} {#MyAppVersion} — контейнерное приложение.' + #13#10#13#10 +
    'Инсталлятор выполнит следующие действия:' + #13#10 +
    '  — проверит, установлен ли Docker Desktop и запущен ли он' + #13#10 +
    '  — загрузит образы приложения (images.tar)' + #13#10 +
    '  — запустит 3 контейнера (frontend, backend, база данных)' + #13#10 +
    '  — дождётся, пока каждый контейнер перейдёт в состояние «healthy»' + #13#10#13#10 +
    'Подробный журнал установки записывается в файл install.log в каталоге установки.');

  AccessPage := CreateInputQueryPage(InfoPage.ID,
    'Параметры доступа',
    'Укажите адреса и порты для доступа к приложению',
    'Эти значения используются для настройки сети и формирования ссылок.' + #13#10 +
    'Для локальной установки оставьте значения по умолчанию (127.0.0.1).');
  AccessPage.Add('Адрес бэкенда:', False);
  AccessPage.Add('Порт:', False);
  AccessPage.Add('Адрес фронтенда:', False);
  AccessPage.Add('Порт:', False);
  AccessPage.Add('Порт базы данных:', False);
  AccessPage.Values[0] := '127.0.0.1';
  AccessPage.Values[1] := '8496';
  AccessPage.Values[2] := '127.0.0.1';
  AccessPage.Values[3] := '4963';
  AccessPage.Values[4] := '5496';

  { --- Кастомная горизонтальная компоновка полей, чтобы всё поместилось --- }
  Gap := ScaleX(16);
  PortWidth := ScaleX(100);
  HostWidth := AccessPage.Surface.Width - PortWidth - Gap;
  PortX := HostWidth + Gap;

  { Строка 1: Бэкенд }
  Y := AccessPage.PromptLabels[0].Top;

  AccessPage.PromptLabels[0].Left := 0;
  AccessPage.PromptLabels[0].Top := Y;
  AccessPage.PromptLabels[0].Width := HostWidth;

  AccessPage.PromptLabels[1].Left := PortX;
  AccessPage.PromptLabels[1].Top := Y;
  AccessPage.PromptLabels[1].Width := PortWidth;

  Y := Y + AccessPage.PromptLabels[0].Height + ScaleY(2);

  AccessPage.Edits[0].Left := 0;
  AccessPage.Edits[0].Top := Y;
  AccessPage.Edits[0].Width := HostWidth;

  AccessPage.Edits[1].Left := PortX;
  AccessPage.Edits[1].Top := Y;
  AccessPage.Edits[1].Width := PortWidth;

  { Строка 2: Чекбокс }
  Y := Y + AccessPage.Edits[0].Height + ScaleY(12);

  AdvancedCheck := TCheckBox.Create(AccessPage.Surface);
  AdvancedCheck.Parent  := AccessPage.Surface;
  AdvancedCheck.Caption := 'Настроить дополнительные параметры (Фронтенд и База данных)';
  AdvancedCheck.Left    := 0;
  AdvancedCheck.Top     := Y;
  AdvancedCheck.Width   := AccessPage.Surface.Width;
  AdvancedCheck.OnClick := @AdvancedCheckClick;
  AdvancedCheck.Checked := False;

  { Строка 3: Фронтенд }
  Y := Y + AdvancedCheck.Height + ScaleY(16);

  AccessPage.PromptLabels[2].Left := 0;
  AccessPage.PromptLabels[2].Top := Y;
  AccessPage.PromptLabels[2].Width := HostWidth;

  AccessPage.PromptLabels[3].Left := PortX;
  AccessPage.PromptLabels[3].Top := Y;
  AccessPage.PromptLabels[3].Width := PortWidth;

  Y := Y + AccessPage.PromptLabels[2].Height + ScaleY(2);

  AccessPage.Edits[2].Left := 0;
  AccessPage.Edits[2].Top := Y;
  AccessPage.Edits[2].Width := HostWidth;

  AccessPage.Edits[3].Left := PortX;
  AccessPage.Edits[3].Top := Y;
  AccessPage.Edits[3].Width := PortWidth;

  { Строка 4: База данных }
  Y := Y + AccessPage.Edits[2].Height + ScaleY(12);

  AccessPage.PromptLabels[4].Left := 0;
  AccessPage.PromptLabels[4].Top := Y;
  AccessPage.PromptLabels[4].Width := PortWidth;

  Y := Y + AccessPage.PromptLabels[4].Height + ScaleY(2);

  AccessPage.Edits[4].Left := 0;
  AccessPage.Edits[4].Top := Y;
  AccessPage.Edits[4].Width := PortWidth;

  { Инициализируем видимость полей }
  AdvancedCheckClick(nil);
  { ----------------------------------------------------------------------- }

  ProductsPage := CreateOutputMsgPage(AccessPage.ID,
    'Другие продукты от {#MyAppPublisher}',
    'Возможно, вас заинтересует',
    'Ознакомьтесь с другими нашими продуктами:' + #13#10#13#10 +
    '  — Product A — https://example.com/a' + #13#10 +
    '  — Product B — https://example.com/b' + #13#10 +
    '  — Product C — https://example.com/c' + #13#10#13#10);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  BPort, FPort, DbPort: Integer;
begin
  Result := True;
  if (AccessPage <> nil) and (CurPageID = AccessPage.ID) then
  begin
    AccessPage.Values[0] := Trim(AccessPage.Values[0]);
    AccessPage.Values[2] := Trim(AccessPage.Values[2]);

    if not IsValidHostOrIp(AccessPage.Values[0]) then
    begin
      MsgBox('Некорректный адрес бэкенда. Укажите IP-адрес или имя хоста.', mbError, MB_OK);
      Result := False;
      Exit;
    end;

    BPort := StrToIntDef(AccessPage.Values[1], 0);
    if (BPort < 1) or (BPort > 65535) then
    begin
      MsgBox('Некорректный порт бэкенда. Укажите число от 1 до 65535.', mbError, MB_OK);
      Result := False;
      Exit;
    end;

    if AdvancedCheck.Checked then
    begin
      if not IsValidHostOrIp(AccessPage.Values[2]) then
      begin
        MsgBox('Некорректный адрес фронтенда. Укажите IP-адрес или имя хоста.', mbError, MB_OK);
        Result := False;
        Exit;
      end;

      FPort := StrToIntDef(AccessPage.Values[3], 0);
      if (FPort < 1) or (FPort > 65535) then
      begin
        MsgBox('Некорректный порт фронтенда. Укажите число от 1 до 65535.', mbError, MB_OK);
        Result := False;
        Exit;
      end;

      DbPort := StrToIntDef(AccessPage.Values[4], 0);
      if (DbPort < 1) or (DbPort > 65535) then
      begin
        MsgBox('Некорректный порт базы данных. Укажите число от 1 до 65535.', mbError, MB_OK);
        Result := False;
        Exit;
      end;
    end;
  end;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ErrMsg, ParentDir, AppDir: String;
begin
  Result := '';

  AppDir    := ExpandConstant('{app}');
  ParentDir := ExtractFileDir(AppDir);

  if not DirExists(ParentDir) then
  begin
    Result := 'Каталог для установки не существует:' + #13#10 + ParentDir;
    Exit;
  end;

  if not IsDirWritable(ParentDir) then
  begin
    Result :=
      'Нет прав на запись в каталог:' + #13#10 + ParentDir + #13#10#13#10 +
      'Пожалуйста, переместите инсталлятор в каталог с правами на запись ' +
      '(например, на Рабочий стол, в Документы или на USB-накопитель) ' +
      'и запустите его снова.';
    Exit;
  end;

  if DirExists(AppDir) and (not IsDirWritable(AppDir)) then
  begin
    Result :=
      'Нет прав на запись в каталог установки:' + #13#10 + AppDir + #13#10#13#10 +
      'Удалите его вручную или запустите инсталлятор от имени пользователя с правами на запись.';
    Exit;
  end;

  if not CheckDocker(ErrMsg) then
    Result := ErrMsg;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ExitCode: Integer;
  Tail, LogFile: String;
begin
  if CurStep <> ssPostInstall then Exit;

  InstallSucceeded := False;
  LogFile := ExpandConstant('{app}\install.log');
  if FileExists(LogFile) and (not DeleteFile(LogFile)) then
    Log('Предупреждение: не удалось удалить старый install.log (возможно, файл занят).');

  WizardForm.StatusLabel.Caption := 'Подготовка к развёртыванию контейнеров...';
  WizardForm.StatusLabel.Update;

  if not RunDeployment(ExitCode) then
  begin
    MsgBox('Не удалось получить результат развёртывания.' + #13#10 +
           'Проверьте файл install.log в каталоге установки.',
           mbError, MB_OK);
    Exit;
  end;

  if ExitCode <> 0 then
  begin
    Tail := GetTail(LogFile, 25);
    if Tail = '' then Tail := '(журнал пуст)';

    case ExitCode of
      10: Tail := 'Docker не установлен или отсутствует в PATH.'              + #13#10#13#10 + Tail;
      11: Tail := 'Демон Docker не запущен.'                                  + #13#10#13#10 + Tail;
      12: Tail := 'Плагин docker compose недоступен.'                         + #13#10#13#10 + Tail;
      13: Tail := 'Файл images.tar не найден в каталоге установки.'           + #13#10#13#10 + Tail;
      14: Tail := 'Не удалось загрузить образы из images.tar.'                + #13#10#13#10 + Tail;
      20: Tail := 'Не удалось запустить один или несколько контейнеров.'      + #13#10#13#10 + Tail;
      21: Tail := 'Контейнеры не перешли в состояние «healthy» за отведённое время.' + #13#10#13#10 + Tail;
    end;

    MsgBox('Установка завершилась с ошибкой (код ' + IntToStr(ExitCode) + ').' + #13#10#13#10 +
           Tail + #13#10 +
           'Полный журнал: ' + LogFile,
           mbError, MB_OK);

    if MsgBox('Открыть файл журнала?', mbConfirmation, MB_YESNO) = IDYES then
      ShellExec('open', LogFile, '', '', SW_SHOWNORMAL, ewNoWait, ExitCode);
  end
  else
  begin
    InstallSucceeded := True;
  end;
end;

procedure CurPageChanged(CurPageID: Integer);
var
  FrontendUrl: String;
begin
  if CurPageID = wpInstalling then
    LayoutCarousel;

  if CurPageID = wpFinished then
  begin
    if InstallSucceeded then
    begin
      { Динамически формируем URL на основе введенных данных }
      FrontendUrl := 'http://' + AccessPage.Values[2] + ':' + AccessPage.Values[3];

      WizardForm.FinishedLabel.Visible := False;
      FinishedLinks.Caption :=
        'Установка «{#MyAppName}» успешно завершена!' + #13#10#13#10 +
        'Приложение доступно по следующей ссылке:' + #13#10 +
        '  • Веб-интерфейс: <a href="' + FrontendUrl + '">' + FrontendUrl + '</a>' + #13#10 +
        'Подробный журнал установки: install.log';
      FinishedLinks.Visible := True;
    end
    else
    begin
      FinishedLinks.Visible := False;
      WizardForm.FinishedLabel.Visible := True;
      WizardForm.FinishedLabel.Caption :=
        'Установка завершена с ошибками.' + #13#10#13#10 +
        'Подробности см. в файле install.log в каталоге установки.';
    end;
  end;
end;
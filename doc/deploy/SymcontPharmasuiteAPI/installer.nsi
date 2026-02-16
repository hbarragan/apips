Unicode true

!include "MUI2.nsh"
!include "nsDialogs.nsh"
!include "LogicLib.nsh"

Name "Adasoft - Symcont Pharmasuite API"
OutFile "SymcontPharmasuiteAPI.exe"
InstallDir "$PROGRAMFILES64\Adasoft\SymcontPharmasuiteAPI"
RequestExecutionLevel admin

ShowInstDetails show

Page directory
Page custom VersionPageCreate VersionPageLeave
Page custom EnableCertPageCreate EnableCertPageLeave
Page custom CertPageCreate CertPageLeave
Page instfiles

Var Dialog

Var ComboVersion
Var RadioCertYes
Var RadioCertNo

Var ComboTypeSSL
Var TextCertStore
Var BtnBrowseCert
Var TextCertPassword

Var VERSION
Var ENABLE_CERT
Var TYPE_SSL
Var CERT_STORE
Var CERT_PSWD_SSL

Function VersionPageCreate
  nsDialogs::Create 1018
  Pop $Dialog
  ${If} $Dialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 10u 100% 12u "Which Pharmasuite version do you want to install?"
  Pop $0

  ${NSD_CreateComboBox} 0 30u 100% 60u ""
  Pop $ComboVersion
  ${NSD_CB_AddString} $ComboVersion "ps11"
  ${NSD_CB_AddString} $ComboVersion "ps10"
  ${NSD_CB_SelectString} $ComboVersion "ps11"

  nsDialogs::Show
FunctionEnd

Function VersionPageLeave
  ${NSD_GetText} $ComboVersion $VERSION
FunctionEnd

Function BrowseCertFile
  nsDialogs::SelectFileDialog open "" "All files (*.*)|*.*"
  Pop $0

  ${If} $0 != ""
    ${NSD_SetText} $TextCertStore $0
  ${EndIf}
FunctionEnd


Function EnableCertPageCreate
  nsDialogs::Create 1018
  Pop $Dialog
  ${If} $Dialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 10u 100% 12u "Install with client certificate?"
  Pop $0

  ${NSD_CreateRadioButton} 10u 30u 100% 12u "Yes"
  Pop $RadioCertYes

  ${NSD_CreateRadioButton} 10u 45u 100% 12u "No"
  Pop $RadioCertNo

  ${NSD_Check} $RadioCertYes

  nsDialogs::Show
FunctionEnd

Function EnableCertPageLeave
  ${NSD_GetState} $RadioCertYes $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $ENABLE_CERT "1"
  ${Else}
    StrCpy $ENABLE_CERT "0"
  ${EndIf}
FunctionEnd

Function CertPageCreate
  nsDialogs::Create 1018
  Pop $Dialog
  ${If} $Dialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 10u 100% 12u "Certificate type (TYPE_SSL):"
  Pop $0

  ${NSD_CreateComboBox} 0 25u 100% 60u ""
  Pop $ComboTypeSSL
  ${NSD_CB_AddString} $ComboTypeSSL "PKCS12"
  ${NSD_CB_AddString} $ComboTypeSSL "JKS"
  ${NSD_CB_AddString} $ComboTypeSSL "PEM"
  
  ${NSD_CB_SelectString} $ComboTypeSSL "PKCS12"


	${NSD_CreateLabel} 0 60u 100% 12u "Certificate path (CERT_STORE):"
	Pop $0

	${NSD_CreateText} 0 75u 80% 12u ""
	Pop $TextCertStore

	${NSD_CreateButton} 82% 75u 18% 12u "Browse..."
	Pop $BtnBrowseCert

	${NSD_OnClick} $BtnBrowseCert BrowseCertFile


  ${NSD_CreateLabel} 0 100u 100% 12u "Certificate password (CERT_PSWD_SSL):"
  Pop $0

  ${NSD_CreatePassword} 0 115u 100% 12u ""
  Pop $TextCertPassword

  nsDialogs::Show
FunctionEnd

Function CertPageLeave

  ${NSD_GetText} $ComboTypeSSL $TYPE_SSL
  ${NSD_GetText} $TextCertStore $CERT_STORE
  ${NSD_GetText} $TextCertPassword $CERT_PSWD_SSL

  ${If} $ENABLE_CERT != "1"
    ; No certificate → ignore everything
    StrCpy $TYPE_SSL ""
    StrCpy $CERT_STORE ""
    StrCpy $CERT_PSWD_SSL ""
    Return
  ${EndIf}

  ; Optional validation (NO BLOQUEANTE)
  ; Puedes comentar esto si quieres 0 validación
  ${If} $TYPE_SSL == ""
    MessageBox MB_ICONEXCLAMATION "Certificate enabled but TYPE_SSL is empty."
  ${EndIf}

FunctionEnd


Section "Install"

  SetOutPath "$INSTDIR"

  ; Copy app (exclude config.ini if exists)
  File /r /x config.ini "app\*"

  FileOpen $0 "$INSTDIR\config.ini" w
  FileWrite $0 "ENABLE_CERT=$ENABLE_CERT$\r$\n"
  FileWrite $0 "CERT_STORE=$CERT_STORE$\r$\n"
  FileWrite $0 "CERT_PSWD_SSL=$CERT_PSWD_SSL$\r$\n"
  FileWrite $0 "TYPE_SSL=$TYPE_SSL$\r$\n"
  FileWrite $0 "VERSION=$VERSION$\r$\n"
  FileWrite $0 "; VERSION=ps10$\r$\n"
  FileClose $0

; -----------------------------
  ; Run installer BAT (visible window)
  ; -----------------------------
  DetailPrint "Running installation script..."
  ExecWait 'cmd.exe /c "$INSTDIR\install-or-update-service.bat"' $0



  ; -----------------------------
  ; Close installer
  ; -----------------------------
	MessageBox MB_OK "Installation finished successfully."

	Exec '"$INSTDIR\install.log"'
	Quit
SectionEnd
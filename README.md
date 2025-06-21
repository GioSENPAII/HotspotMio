# 📱 Bluetooth Hotspot App

Una aplicación Android que permite compartir conexión a internet a través de Bluetooth, funcionando como un hotspot que procesa búsquedas web y devuelve resultados a dispositivos cliente conectados.

## 🌟 Características Principales

- **Modo Host**: Crea un servidor Bluetooth que acepta conexiones y procesa búsquedas web
- **Mode Cliente**: Se conecta a un dispositivo host para realizar búsquedas
- **Búsquedas en tiempo real**: Utiliza DuckDuckGo para obtener resultados de búsqueda
- **Notificaciones inteligentes**: Sistema completo de notificaciones para ambos modos
- **Monitoreo avanzado**: Logs técnicos detallados y estadísticas en tiempo real
- **Temas personalizables**: Soporte para temas IPN y ESCOM
- **Historial completo**: Registro de todas las búsquedas realizadas

## 📋 Requisitos del Sistema

- **Android 7.0 (API 24)** o superior
- **Bluetooth 4.0** o superior
- **Permisos de Bluetooth** (se solicitan automáticamente)
- **Permisos de notificación** (Android 13+)
- **Conexión a internet** (solo para el dispositivo host)

## 🚀 Instalación

### Opción 1: Instalación desde código fuente

```bash
# Clonar el repositorio
git clone https://github.com/tuusuario/HotspotMio.git

# Abrir en Android Studio
cd HotspotMio
# Abrir con Android Studio

# Compilar e instalar
./gradlew installDebug
```

### Opción 2: APK Release
*(Próximamente disponible en releases)*

## 📖 Guía de Uso

### 🎯 Configuración como Host (Servidor)

1. **Iniciar la aplicación** y seleccionar "Iniciar Servidor Host"
2. **Conceder permisos** de Bluetooth y notificaciones cuando se soliciten
3. **Hacer el dispositivo visible** para otros dispositivos Bluetooth
4. **Presionar "Iniciar Servidor"** en la pantalla del Host
5. **Monitorear conexiones** a través de las pestañas:
   - 📱 **Monitor**: Estado en tiempo real de clientes conectados
   - 📋 **Historial**: Registro de todas las búsquedas procesadas
   - 🔧 **Logs**: Información técnica detallada del servidor

### 📲 Configuración como Cliente

1. **Emparejar dispositivos** Bluetooth previamente (Configuración → Bluetooth)
2. **Abrir "Cliente Bluetooth"** desde el menú principal
3. **Presionar "Conectar con Host"** y seleccionar el dispositivo host
4. **Realizar búsquedas** una vez conectado
5. **Ver resultados** en tiempo real

## 🏗️ Arquitectura Técnica

### 📦 Estructura del Proyecto

```
app/src/main/java/com/example/bluetoothhotspotapp/
├── 📁 data/
│   ├── 📁 model/          # Modelos de datos (SearchResult, SearchHistory)
│   ├── 📁 network/        # Servicios de red y parsing HTML
│   └── 📁 repository/     # Gestión de comunicación Bluetooth
├── 📁 ui/                 # Activities y Adapters
├── 📁 viewmodel/          # ViewModels para MVVM
├── 📁 notification/       # Sistema de notificaciones
├── 📁 di/                 # Inyección de dependencias
└── 📁 util/               # Utilidades (JSON, etc.)
```

### 🔧 Componentes Principales

#### HostService
- **Servicio en primer plano** que mantiene el servidor Bluetooth activo
- **Procesamiento de búsquedas** utilizando Retrofit y JSoup
- **Comunicación bidireccional** con múltiples clientes
- **Logs detallados** de todas las operaciones

#### BluetoothClientCommunicationManager
- **Gestión de conexiones** Bluetooth del lado cliente
- **Protocolo de comunicación** robusto con manejo de errores
- **Keep-alive automático** para mantener conexiones estables
- **Recepción optimizada** de datos JSON

#### Sistema de Notificaciones
- **Notificaciones contextuales** para eventos importantes
- **Canales separados** para diferentes tipos de notificaciones
- **Soporte completo** para Android 13+ y versiones anteriores

## 🛠️ Tecnologías Utilizadas

### Core Android
- **Kotlin** - Lenguaje principal
- **Android SDK 35** - API target
- **ViewBinding** - Vinculación de vistas
- **Material Design 3** - Diseño moderno

### Arquitectura
- **MVVM Pattern** - Separación de responsabilidades
- **Coroutines** - Programación asíncrona
- **StateFlow/SharedFlow** - Gestión reactiva de estado
- **Dependency Injection** - Inyección manual optimizada

### Networking
- **Retrofit 2.9.0** - Cliente HTTP
- **JSoup 1.17.2** - Parsing HTML
- **Gson 2.10.1** - Serialización JSON

### Bluetooth
- **BluetoothAdapter** - Gestión de Bluetooth nativo
- **RFCOMM Protocol** - Comunicación serie sobre Bluetooth
- **UUID estándar** - Identificación de servicios

## ⚙️ Configuración del Entorno

### Variables de Configuración

El proyecto utiliza las siguientes configuraciones principales:

```kotlin
// Constants.kt
object Constants {
    val BLUETOOTH_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    const val SERVICE_NAME = "BluetoothHotspotService"
}
```

### Permisos Requeridos

```xml
<!-- Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Notificaciones -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Internet y Servicios -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## 🎨 Personalización de Temas

La aplicación soporta dos temas personalizables:

### Tema IPN (Guinda)
```xml
<style name="Theme.IPN" parent="Theme.MaterialComponents.Light.NoActionBar">
    <item name="colorPrimary">@color/ipn_primary</item>
    <!-- Color principal: #8A1538 -->
</style>
```

### Tema ESCOM (Azul)
```xml
<style name="Theme.ESCOM" parent="Theme.MaterialComponents.Light.NoActionBar">
    <item name="colorPrimary">@color/escom_primary</item>
    <!-- Color principal: #003366 -->
</style>
```

## 🔍 API de Búsqueda

La aplicación utiliza la API HTML de DuckDuckGo para realizar búsquedas:

```kotlin
@GET("/html/")
suspend fun search(
    @Query("q") query: String,
    @Header("User-Agent") userAgent: String
): String
```

**Endpoint**: `https://html.duckduckgo.com/html/`

## 📊 Monitoreo y Logs

### Tipos de Logs Disponibles

1. **Logs de Conexión**: Estados de Bluetooth y conexiones de clientes
2. **Logs de Búsqueda**: Queries procesadas y resultados enviados
3. **Logs de Performance**: Tiempos de respuesta y bytes transferidos
4. **Logs de Errores**: Excepciones y problemas de conectividad

### Estadísticas en Tiempo Real

- Total de conexiones atendidas
- Número de búsquedas procesadas
- Bytes totales transferidos
- Tiempo promedio de respuesta

## 🚨 Solución de Problemas

### Problemas Comunes

#### ❌ "No se puede conectar al host"
**Solución**: 
- Verificar que ambos dispositivos tengan Bluetooth activado
- Asegurar que los dispositivos estén emparejados previamente
- Reiniciar el servicio host

#### ❌ "Sin resultados de búsqueda"
**Solución**:
- Verificar conexión a internet en el dispositivo host
- Comprobar que el servicio host esté activo
- Revisar logs técnicos para errores específicos

#### ❌ "Notificaciones no aparecen"
**Solución**:
- Conceder permisos de notificación en Configuración
- Verificar que las notificaciones no estén silenciadas
- Reiniciar la aplicación

### Logs de Debugging

Para debugging avanzado, consultar los logs del sistema:

```bash
adb logcat | grep "HostService\|ClientBT"
```

## 🤝 Contribución

### Cómo Contribuir

1. **Fork** el repositorio
2. **Crear** una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. **Commit** tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. **Push** a la rama (`git push origin feature/nueva-funcionalidad`)
5. **Abrir** un Pull Request

### Estándares de Código

- **Kotlin Coding Conventions**
- **Comentarios en español** para documentación de usuario
- **Nombres descriptivos** para variables y funciones
- **Arquitectura MVVM** consistente

## 👥 Equipo de Desarrollo

- **Desarrollador Principal**: [Tu Nombre]
- **Institución**: Instituto Politécnico Nacional
- **Escuela**: ESCOM (Escuela Superior de Cómputo)

## 📞 Soporte

Para soporte técnico o reportar bugs:

- **Issues**: [GitHub Issues](https://github.com/GioSENPAII/HotspotMio/issues)
- **Documentación**: [Wiki del proyecto](https://github.com/GioSENPAII/HotspotMio/wiki)

## 🔄 Changelog

### v1.0.0 (Actual)
- ✅ Implementación completa del protocolo Bluetooth
- ✅ Sistema de búsquedas con DuckDuckGo
- ✅ Notificaciones inteligentes
- ✅ Monitoreo en tiempo real
- ✅ Soporte para múltiples temas
- ✅ Logs técnicos detallados

### Próximas Funcionalidades
- 🔄 Soporte para múltiples clientes simultáneos
- 🔄 Caché de búsquedas frecuentes
- 🔄 Configuración avanzada de red
- 🔄 Exportación de logs e historial

---

**⭐ Si este proyecto te resulta útil, ¡no olvides darle una estrella!**

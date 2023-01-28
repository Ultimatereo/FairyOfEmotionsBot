import java.nio.file.Path
import java.util.*

class ProjectProperties {
    companion object {
        val mainProperties: Properties
            get() {
                val properties = Properties()
                try {
                    properties.load(ProjectProperties::class.java.getResourceAsStream("/main.properties"))
                } catch (e: Exception) {
                    throw NoSuchFileException(Path.of("src/main/resources/main.properties").toFile())
                }
                return properties
            }
    }
}
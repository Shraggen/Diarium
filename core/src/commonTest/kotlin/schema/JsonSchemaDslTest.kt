import com.shraggen.diarium.schema.JsonIntegerSchema
import com.shraggen.diarium.schema.JsonObjectSchema
import com.shraggen.diarium.schema.JsonStringSchema
import com.shraggen.diarium.schema.obj
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonSchemaDslTest {

    @Test
    fun buildsObjectSchema() {

        val schema =
            obj {

                string("city")

                integer("days")

                required("city")
            }

        assertEquals(
            setOf("city"),
            schema.required,
        )

        assertTrue(schema.properties["city"] is JsonStringSchema)

        assertTrue(schema.properties["days"] is JsonIntegerSchema)
    }

    @Test
    fun supportsNestedObjects() {

        val schema =
            obj {

                objectProperty("location") {

                    string("city")

                    string("country")

                    required("city")
                }
            }

        val nested =
            schema.properties["location"] as JsonObjectSchema

        assertEquals(
            2,
            nested.properties.size,
        )
    }
}

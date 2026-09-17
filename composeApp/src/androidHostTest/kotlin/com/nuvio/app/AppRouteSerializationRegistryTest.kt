package com.nuvio.app

import androidx.navigation3.runtime.NavKey
import com.nuvio.app.navigation.AppRoute
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `AppRoute` subclasses are registered one by one in [navigationSavedStateConfiguration]'s
 * polymorphic module — nothing enforces that at compile time, so a forgotten `subclass(...)` call
 * only surfaces as a runtime `SerializationException` the first time that route is pushed (see
 * `LibraryRatedRoute`, which crashed exactly this way). This test walks every sealed subclass of
 * `AppRoute` reflectively and fails if one isn't registered.
 */
class AppRouteSerializationRegistryTest {

    @Test
    fun everyAppRouteSubclassIsRegisteredForPolymorphicSerialization() {
        val registered = mutableSetOf<KClass<*>>()
        navigationSavedStateConfiguration.serializersModule.dumpTo(
            object : SerializersModuleCollector {
                override fun <T : Any> contextual(
                    kClass: KClass<T>,
                    provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
                ) = Unit

                override fun <Base : Any, Sub : Base> polymorphic(
                    baseClass: KClass<Base>,
                    actualClass: KClass<Sub>,
                    actualSerializer: KSerializer<Sub>,
                ) {
                    if (baseClass == NavKey::class) registered += actualClass
                }

                override fun <Base : Any> polymorphicDefaultSerializer(
                    baseClass: KClass<Base>,
                    defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
                ) = Unit

                override fun <Base : Any> polymorphicDefaultDeserializer(
                    baseClass: KClass<Base>,
                    defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<out Base>?,
                ) = Unit
            },
        )

        val expected = allConcreteAppRouteSubclasses(AppRoute::class)
        val missing = expected - registered

        assertTrue(
            missing.isEmpty(),
            "AppRoute subclasses missing a subclass(...) registration in " +
                "navigationSavedStateConfiguration's SerializersModule: " +
                missing.joinToString { it.simpleName ?: it.toString() },
        )
    }
}

@Suppress("UNCHECKED_CAST")
private fun allConcreteAppRouteSubclasses(base: KClass<out AppRoute>): Set<KClass<out AppRoute>> =
    base.sealedSubclasses.flatMap { sub ->
        if (sub.isSealed) allConcreteAppRouteSubclasses(sub as KClass<out AppRoute>) else setOf(sub)
    }.toSet()

package com.borgnetzwerk.searchsnail.configuration

import com.borgnetzwerk.searchsnail.controller.domain.WikiDataLiteralGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiDataResourceGraphQL
import graphql.schema.TypeResolver
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.ClassNameTypeResolver
import org.springframework.graphql.execution.RuntimeWiringConfigurer

// TODO look into UnionType resolver
@Configuration
class GraphQLConfig /*: RuntimeWiringConfigurer */ {

    /*
    override fun configure(builder: RuntimeWiring.Builder) {
        val typeResolver = ClassNameTypeResolver()
        typeResolver.addMapping(WikiDataResourceGraphQL::class.java, "WikiDataResource")

        val runtimeWiring = TypeRuntimeWiring.Builder().typeName("WikiDataResource").typeResolver(typeResolver).build()
        builder.type(runtimeWiring)
    }
    */

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { wiringBuilder ->

            val mediaTypeResolver = TypeResolver { env ->
                val javaObject = env.getObject<Any>()

                when (javaObject) {
                    is WikiDataResourceGraphQL -> env.schema.getObjectType("WikiDataResource")
                    is WikiDataLiteralGraphQL -> env.schema.getObjectType("WikiDataLiteral")
                    else -> throw RuntimeException("Unexpected type: ${javaObject::class}")
                }
            }

            wiringBuilder.type("WikiData") { typeWiring ->
                typeWiring.typeResolver(mediaTypeResolver)
            }
        }
    }
}
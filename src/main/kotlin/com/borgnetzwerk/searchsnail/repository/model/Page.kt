package com.borgnetzwerk.searchsnail.repository.model

data class Page<T>(
    val offset: Int = -1, // zero-based index (first element in list comes after offset index)
    val total: Int?,
    val elements: List<T>,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
) {
    fun endOffset(): Int = offset + elements.size

    inline fun <K> toPageMap(provenance: String, toKey: (element: T) -> K): PageMap<K, T> {
        val mutableMap = mutableMapOf<K, List<IndexedElement<T>>>()

        elements.forEachIndexed { index, element ->
            val key = toKey(element)

            mutableMap[key]?.let {
                mutableMap[key] = listOf(IndexedElement(offset + index, element, provenance)).optionalPlus(it)
            } ?: let {
                mutableMap[key] = listOf(IndexedElement(offset + index, element, provenance))
            }
        }
        return PageMap(
            mutableMap,
            hasNextPage,
            hasPreviousPage
        )
    }
}

data class IndexedPage<T>(
    val elements: List<IndexedElement<T>>,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val offset: Int,
    val limit: Int,
)

data class IndexedElement<T>(val index: Int, val value: T, val provenance: String)

/*
data class IndexedElement<T : Comparable<T>>(val index: Int, val value: T, val provenance: String) : Comparable<IndexedElement<T>> {
    override fun compareTo(other: IndexedElement<T>): Int = value.compareTo(other.value)
}

 */


// new idea LinkedList model with global index
// keeps order
// value in node can be replaced -> keeps order
// can be easily merged with other
// removing -> keeps order
// just keep attention to not swap values
// last and first node could be a Meta-Node about hasPrevious

data class PageMap<K, T>(
    val page: MutableMap<K, List<IndexedElement<T>>>,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
) {
    fun getHighestIndexedItem(): IndexedElement<T>? = page.entries.fold(null) { acc: IndexedElement<T>?, mutableEntry ->
        mutableEntry.value.fold(null) { listAcc: IndexedElement<T>?, indexedElement ->
            if (listAcc !== null && listAcc.index > indexedElement.index) {
                listAcc
            } else {
                indexedElement
            }
        }.let { localAcc ->
            if (acc !== null && localAcc !== null && acc.index > localAcc.index) {
                acc
            } else {
                localAcc
            }
        }
    }

    inline fun filterByKey(predicate: (key: K) -> Boolean): PageMap<K, T> {
        val mutableMap = mutableMapOf<K, List<IndexedElement<T>>>()
        page.keys.forEach { key ->
            if(predicate(key)){
                mutableMap[key] = page[key].orEmpty()
            }
        }
        return PageMap(
            mutableMap,
            hasNextPage,
            hasPreviousPage
        )
    }

    inline fun <R> mapValues(transform: (key: K, element: T) -> R): PageMap<K, R> {
        val mutableMap = mutableMapOf<K, List<IndexedElement<R>>>()
        page.entries.forEach { (key, values) -> mutableMap[key] = values.map { it -> IndexedElement(it.index, transform(key, it.value), it.provenance ) } }
        return PageMap(
            mutableMap,
            hasNextPage,
            hasPreviousPage
        )
    }
}

data class MultiPageMap<P, K, T>(
    val pages: MutableMap<P, PageMap<K, T>>,
) {
    inline fun <R> flatmap(callback: (page: P, key: K, value: IndexedElement<T>) -> R): List<R> =
        pages.entries.flatMap { page ->
            page.value.page.entries.flatMap { elements ->
                elements.value.map { element -> callback(page.key, elements.key, element) }
            }
        }

    fun add(page: P, key: K, value: IndexedElement<T>): Unit {
        pages[page]?.page?.let { p -> p[key] = listOf(value).optionalPlus(p[key]) }
    }

    fun remove(page: P, key: K): Unit {
        pages[page]?.page?.remove(key)
    }

    inline fun filter(predicate: (page: P, key: K, value: IndexedElement<T>) -> Boolean): MultiPageMap<P, K, T> {
        val otherMultiPageMap = MultiPageMap(mutableMapOf<P, PageMap<K, T>>())
        for (p in pages.entries) {
            otherMultiPageMap.pages[p.key] = PageMap(mutableMapOf(), p.value.hasNextPage, p.value.hasPreviousPage)
        }
        flatmap { page, key, value ->
            if (predicate(page, key, value)) {
                otherMultiPageMap.add(page, key, value)
            }
        }
        return otherMultiPageMap
    }

}

fun <T> List<IndexedElement<T>>.optionalPlus(b: List<IndexedElement<T>>?): List<IndexedElement<T>> = if (b != null) {
    this.plus(b)
} else {
    this
}
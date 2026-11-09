<script lang="scala">
// .stで書いた表. RowsScalaと同じDOMを作ります.

def select(id: Int): Unit = Store.selected := id

def remove(id: Int): Unit = Store.rows.rm(Store.rows.v.indexWhere(_.id == id))
</script>

<template>
  <table class="rows">
    <tbody id="tbody">
      <tr v-for="row in Store.rows" :class="Store.selected.mapC(s => if (s == row.v.id) &quot;danger&quot; else &quot;&quot;)">
        <td class="id">{{ row.v.id }}</td>
        <td class="label"><a @click="select(row.v.id)">{{ row.mapC(_.label) }}</a></td>
        <td><a class="remove" @click="remove(row.v.id)">x</a></td>
      </tr>
    </tbody>
  </table>
</template>

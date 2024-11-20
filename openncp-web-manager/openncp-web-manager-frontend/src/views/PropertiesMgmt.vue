<template>
  <v-container fluid>
    <v-card class="mx-auto mt-10" outlined>
      <!--v-card-title>Vuetify Inline Editor Table </v-card-title-->

      <v-data-table :headers="headers" :items="properties" :search="search" :item-class="row_classes" class="elevation-1" fixed-header disable-pagination hide-default-footer>
        <v-divider inset></v-divider>
        <template v-slot:top>
          <!-- add new record toolbar + button -->

          <v-toolbar flat>
            <div class="d-flex w-100">
              <v-text-field v-model="search" append-icon="mdi-magnify" label="Search" dense outlined single-line hide-details></v-text-field>
              <!--v-btn
                color="primary"
                class="ml-2 white--text"
                @click="addNew">
                <v-icon dark>mdi-plus</v-icon>Add
              </v-btn-->
            </div>
          </v-toolbar>
        </template>
        <template v-slot:[`item.name`]="{ item }">
          <!--v-text-field v-model="editedItem.name" :hide-details="true" dense single-line :autofocus="true" v-if="item.id === editedItem.id"></v-text-field>
          <span v-else>{{item.name}}</span-->
          <span>{{item.name}}</span>
        </template>
        <template v-slot:[`item.value`]="{ item }">
          <v-text-field v-model="editedItem.value" :hide-details="true" dense single-line :autofocus="true" v-if="item.id === editedItem.id && item.is_boolean === false"></v-text-field>
          <v-checkbox
            false-value="false"
            true-value="true"
            input-value=editedItem.value
            v-model="editedItem.value" :hide-details="true" dense single-line :autofocus="true" v-else-if="item.id === editedItem.id && item.is_boolean === true"></v-checkbox>
          <span v-else>{{item.value}}</span>
        </template>
        <template v-slot:[`item.actions`]="{ item }">
          <div v-if="item.id === editedItem.id">
            <v-icon color="red" class="mr-3" @click="close">
              mdi-window-close
            </v-icon>
            <v-icon color="green"  @click="save">
              mdi-content-save
            </v-icon>
          </div>
          <div v-else>
            <!-- to edit the record -->
            <v-icon color="green" class="mr-3" @click="editItem(item)" v-if="item.is_editable===true">
              mdi-pencil
            </v-icon>
            <!-- to delete the record -->
            <!--v-icon color="red" @click="deleteItem(item)">
              mdi-delete
            </v-icon-->
          </div>
        </template>

        <!-- RESET Button appears when there is no data to fill the table -->
        <!--
        <template v-slot:no-data>
          <v-btn color="primary" @click="initialize">Reset</v-btn>
        </template>
        -->
      </v-data-table>
    </v-card>
  </v-container>
</template>

<script>
import axios from 'axios'

export default {
  data: () => ({
    search: '',
    headers: [
      {
        text: 'id',
        value: 'id',
        sortable: false,
        filterable: false
      },
      {
        text: 'Name',
        value: 'key',
        sortable: true,
        filterable: true
      },
      {
        text: 'Value',
        value: 'value',
        sortable: false,
        filterable: false
      },
      { text: 'Actions', value: 'actions', sortable: false, width: '100px' } /*,
      {
        text: 'Is SMP',
        value: 'smp',
        sortable: false,
        filterable: false
      } */
    ],
    properties: [],
    editedIndex: -1,
    editedItem: {
      id: 0,
      name: '',
      value: '',
      is_editable: true,
      is_boolean: false
    },
    defaultItem: {
      id: 0,
      name: 'New Item',
      value: '',
      is_editable: true,
      is_boolean: false
    },
    loading: false
  }),
  mounted () {
    this.loading = true
    axios
      .get(process.env.VUE_APP_SERVER_URL + '/api/properties')
      .then(response => {
        this.properties = response.data.map(v => (
          {
            ...v,
            id: response.data.indexOf(v) + 1,
            is_editable: true,
            is_boolean: false
          }
        ))

        this.properties.forEach(child => {
          if (child.key.includes('PatientIdentificationService')) {
            child.is_editable = false
          }
          if (child.key.includes('DispensationService')) {
            child.is_editable = false
          }
          if (child.key.includes('OrderService')) {
            child.is_editable = false
          }
          if (child.key.includes('PatientService')) {
            child.is_editable = false
          }

          if (child.key.includes('APP_BEHIND_PROXY')) {
            child.is_boolean = true
          }
          if (child.key.includes('APP_PROXY_AUTHENTICATED')) {
            child.is_boolean = true
          }
          if (child.key.includes('auditrep.forcewrite')) {
            child.is_boolean = true
          }
          if (child.key.includes('automated.validation')) {
            child.is_boolean = true
          }
          if (child.key.includes('automated.validation.remote')) {
            child.is_boolean = true
          }
          if (child.key.includes('PORTAL_CHECK_PERMISSIONS')) {
            child.is_boolean = true
          }
          if (child.key.includes('secman.cert.validator.checkforkeyusage')) {
            child.is_boolean = true
          }
          if (child.key.includes('secman.sts.checkHostname')) {
            child.is_boolean = true
          }
          if (child.key.includes('WRITE_TEST_AUDITS')) {
            child.is_boolean = true
          }
          if (child.key.includes('ABUSE_SCHEDULER_ENABLE')) {
            child.is_boolean = true
          }
          if (child.key.includes('PORTAL_CCD_ENABLED')) {
            child.is_boolean = true
          }
          if (child.key.includes('PORTAL_CONSENT_ENABLED')) {
            child.is_boolean = true
          }
        })
        this.loading = false
      })
  },
  methods: {
    row_classes (item) {
      if (!item.is_editable) {
        return 'grey'
      }
    },
    editItem (item) {
      this.editedIndex = this.properties.indexOf(item)
      this.editedItem = Object.assign({}, item)
    },
    deleteItem (item) {
      const index = this.properties.indexOf(item)
      confirm('Are you sure you want to delete this item?') && this.properties.splice(index, 1)
    },
    close () {
      setTimeout(() => {
        this.editedItem = Object.assign({}, this.defaultItem)
        this.editedIndex = -1
      }, 300)
    },
    addNew () {
      const addObj = Object.assign({}, this.defaultItem)
      addObj.id = this.properties.length + 1
      this.properties.unshift(addObj)
      this.editItem(addObj)
    },
    save () {
      if (this.editedIndex > -1) {
        Object.assign(this.properties[this.editedIndex], this.editedItem)
        console.log(this.properties[this.editedIndex])
        axios
          .put(process.env.VUE_APP_SERVER_URL + '/api/properties/' + this.properties[this.editedIndex].key + '?value=' + this.properties[this.editedIndex].value)
          .then(response => {
            console.log(response)
          })
      }
      this.close()
    }
  }
}
</script>

<style scoped>
.orange {
  background-color: orange;
}

.green {
  background-color: green;
}

.grey {
  background-color: dimgray;
}
</style>

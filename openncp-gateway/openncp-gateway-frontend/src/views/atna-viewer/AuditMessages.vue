<template>
  <v-container fluid>
    <div>
      <v-breadcrumbs :items="items">
        <template v-slot:divider>
          <v-icon>mdi-forward</v-icon>
        </template>
      </v-breadcrumbs>
    </div>
    <v-card>
      <v-card-text>
        <v-card-title>
          <v-row>
            <v-col>
              <v-combobox
                v-model="searchEventId"
                clearable
                label="Event ID Code"
                :items="EventIdCodeItems"
                single-line
                hide-details
              ></v-combobox>
            </v-col>

            <v-col>
              <v-text-field
                v-model="activeParticipantId"
                clearable
                label="Active Participant ID"
                single-line
                hide-details
              ></v-text-field>
            </v-col>

            <v-col>
              <v-combobox
                v-model="activeTypeCode"
                clearable
                label="Participant Type Code"
                :items="ParticipantTypeCodeItems"
                single-line
                hide-details
              ></v-combobox>
            </v-col>

            <v-col>
              <v-menu
                v-model="searchStartDateMenu"
                :close-on-content-click="false"
                max-width="290"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-text-field
                    clearable
                    readonly
                    :value="searchEventStartDate"
                    :label="searchEventStartDateLabel"
                    v-bind="attrs"
                    v-on="on"
                    @click:clear="searchEventStartDate = null"
                  ></v-text-field>
                </template>
                <v-spacer></v-spacer>
                <!-- <v-date-picker
                  v-model="searchEventStartDate"
                  @change="searchStartDateMenu = false"
                ></v-date-picker> -->
                <input type="datetime-local" v-model="searchEventStartDate" />
              </v-menu>
            </v-col>
            <v-col>
              <v-menu
                v-model="searchEndDateMenu"
                :close-on-content-click="false"
                max-width="290"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-text-field
                    :value="searchEventEndDate"
                    clearable
                    readonly
                    :label="searchEventEndDateLabel"
                    v-bind="attrs"
                    v-on="on"
                    @click:clear="searchEventEndDate = null"
                  ></v-text-field>
                </template>
                <v-spacer></v-spacer>
                <!-- <v-date-picker
                  :min="searchEventStartDate"
                  v-model="searchEventEndDate"
                  @change="searchEndDateMenu = false"
                ></v-date-picker> -->
                <input type="datetime-local" v-model="searchEventEndDate" />
              </v-menu>
            </v-col>
            <v-col><v-btn block @click="searchDataFromApi(true)"> Search </v-btn></v-col>
          </v-row>
        </v-card-title>
        <v-data-table
          :headers="headers"
          :items="messages"
          :disable-items-per-page="true"
          :footer-props="{
            'items-per-page-options': [10]
          }"
          :options.sync="options"
          :server-items-length="totalMessages"
          :loading="loading"
        >
          <template v-slot:[`item.actions`]="{ item }">
            <v-btn
              fab
              x-small
              color="indigo"
              :to="{ name: 'audit-details', params: { id: item.id } }"
            >
              <v-icon>mdi-eye</v-icon>
            </v-btn>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script>
import axios from 'axios'

export default {
  data () {
    return {
      headers: [
        { text: '#', value: 'id', sortable: false },
        { text: 'Event ID Code', value: 'eventId', sortable: false },
        { text: 'Event Type Codes', value: 'eventType', sortable: false },
        {
          text: 'Event Action Code',
          value: 'eventActionCode',
          sortable: false
        },
        { text: 'Event Date Time (UTC)', value: 'eventDateTime', sortable: false },
        {
          text: 'Event Outcome Indicator',
          value: 'eventOutcome',
          sortable: false
        },
        { value: 'actions', sortable: false }
      ],
      messages: [],
      diffUTC: 0,
      totalMessages: 0,
      options: { page: 1, itemsPerPage: 10 },
      loading: false,
      filteredData: false,
      searchStartDateMenu: false,
      searchEndDateMenu: false,
      searchEventStartDateLabel: '',
      searchEventEndDateLabel: '',
      items: [
        {
          text: 'ATNA Viewer',
          disabled: false,
          to: { name: 'audits' },
          exact: true
        },
        {
          text: 'List',
          disabled: false,
          to: { name: 'audits' },
          exact: true
        },
        {
          text: 'Audit messages list',
          disabled: true
        }
      ],
      EventIdCodeItems: [
        'EHDSI-91',
        'EHDSI-92',
        'EHDSI-93',
        'EHDSI-94',
        'EHDSI-96',
        'EHDSI-CF',
        'ITI-38',
        'ITI-39',
        'ITI-40',
        'ITI-41',
        'ITI-55'
      ],
      ParticipantTypeCodeItems: [
        'Medical Doctors',
        'Resident Physician',
        'ServiceConsumer',
        'ServiceProvider'
      ]
    }
  },
  mounted () {
    // this.getDataFromApi()
    const d = new Date()
    const a = d.getUTCHours()
    const b = d.getHours()
    const c = b - a
    this.searchEventStartDateLabel = 'Start Date (UTC ' + (c >= 0 ? '+' : '') + c + 'h)'
    this.searchEventEndDateLabel = 'End Date (UTC ' + (c >= 0 ? '+' : '') + c + 'h)'
  },
  watch: {
    options: {
      handler () {
        if (this.filteredData) {
          this.searchDataFromApi(false)
        } else {
          this.getDataFromApi()
        }
      },
      page: 1,
      itemsPerPage: 10
    }
  },
  computed: {
    searchEventId: {
      get () { return this.$store.getters.searchAtnaOpts.searchEventId },
      set (value) {
        this.$store.commit('searchAtnaOpts', { searchEventId: value })
      }
    },
    activeParticipantId: {
      get () { return this.$store.getters.searchAtnaOpts.activeParticipantId },
      set (value) {
        this.$store.commit('searchAtnaOpts', { activeParticipantId: value })
      }
    },
    activeTypeCode: {
      get () { return this.$store.getters.searchAtnaOpts.activeTypeCode },
      set (value) {
        this.$store.commit('searchAtnaOpts', { activeTypeCode: value })
      }
    },
    searchEventStartDate: {
      get () { return this.$store.getters.searchAtnaOpts.searchEventStartDate },
      set (value) {
        this.$store.commit('searchAtnaOpts', { searchEventStartDate: value })
      }
    },
    searchEventEndDate: {
      get () { return this.$store.getters.searchAtnaOpts.searchEventEndDate },
      set (value) {
        this.$store.commit('searchAtnaOpts', { searchEventEndDate: value })
      }
    },
    minDate () {
      if (this.searchEventStartDate) {
        const d = new Date(this.searchEventStartDate)
        return d.setHours(23, 59, 59, 999)
      } else {
        return ''
      }
    }
  },
  methods: {
    searchDataFromApi (resetToFirstPage) {
      if (!this.loading) {
        this.loading = true
        this.apiCall().then((data) => {
          this.messages = data.data.content
          this.totalMessages = data.data.totalElements
          this.options.page = data.data.number + 1
          this.loading = false
          this.filteredData = true
          if (resetToFirstPage) {
            this.$set(this.options, 'page', 1)
          }
        })
      }
    },
    apiCall () {
      return axios.get(process.env.VUE_APP_SERVER_URL + '/api/atna/search_messages', {
        params: {
          pageNumber: this.options.page - 1,
          size: this.options.itemsPerPage,
          searchEventId: this.searchEventId,
          activeParticipantId: this.activeParticipantId,
          activeTypeCode: this.activeTypeCode,
          searchEventStartDate: this.searchEventStartDate
            ? new Date(this.searchEventStartDate).toISOString()
            : '',
          searchEventEndDate: this.searchEventEndDate
            ? new Date(this.searchEventEndDate).toISOString()
            : ''
        }
      })
    },
    getDataFromApi () {
      if (!this.loading) {
        this.loading = true
        axios.get(process.env.VUE_APP_SERVER_URL + '/api/atna/messages', {
          params: {
            pageNumber: this.options.page - 1,
            size: this.options.itemsPerPage
          }
        }).then((response) => {
          this.messages = response.data.content
          this.totalMessages = response.data.totalElements
          this.options.page = response.data.number + 1
          this.loading = false
        })
      }
    }
  }
}
</script>

<style scoped>
input[type=datetime-local] {
  color: white;
  font-size: 0.9rem;
  background-color: #333;
  padding: 5px 10px;
}
</style>

import { FILTER_DEFINITIONS, ORDER_FIELDS, UI_CONFIG } from './constants.js';

const dom = {
  filterGrid: document.getElementById('filterGrid'),
  orderField: document.getElementById('orderField'),
  orderDirection: document.getElementById('orderDirection'),
  topInput: document.getElementById('topInput'),
  skipInput: document.getElementById('skipInput'),
  countInput: document.getElementById('countInput'),
  searchBtn: document.getElementById('searchBtn'),
  clearBtn: document.getElementById('clearBtn'),
  generatedUrl: document.getElementById('generatedUrl'),
  statusBadge: document.getElementById('statusBadge'),
  requestInfo: document.getElementById('requestInfo'),
  resultSummary: document.getElementById('resultSummary'),
  table: document.getElementById('resultTable'),
  rawJson: document.getElementById('rawJson')
};

const state = {
  filterInputs: new Map()
};

const setStatus = (kind, text) => {
  dom.statusBadge.className = `status ${kind}`;
  dom.statusBadge.textContent = text;
};

const normalizeDate = (value) => {
  if (!value) return '';
  return value.endsWith('Z') ? value : `${value}Z`;
};

const formatFilterClause = (definition, rawValue) => {
  const value = rawValue.trim();
  if (!value) return null;

  if (definition.operator === 'contains' || definition.operator === 'startswith') {
    return `${definition.operator}(${definition.field},'${value}')`;
  }

  if (definition.type === 'number') {
    return `${definition.field} ${definition.operator} ${value}`;
  }

  if (definition.type === 'datetime') {
    return `${definition.field} ${definition.operator} ${normalizeDate(value)}`;
  }

  return `${definition.field} ${definition.operator} '${value}'`;
};

const buildQuery = () => {
  const params = new URLSearchParams();
  const clauses = [];

  FILTER_DEFINITIONS.forEach((definition) => {
    const input = state.filterInputs.get(definition.id);
    if (!input) return;
    const clause = formatFilterClause(definition, input.value || '');
    if (clause) clauses.push(clause);
  });

  if (clauses.length > 0) {
    params.set('$filter', clauses.join(' and '));
  }

  const orderField = dom.orderField.value;
  const orderDirection = dom.orderDirection.value;
  if (orderField) {
    params.set('$orderby', `${orderField} ${orderDirection}`);
  }

  params.set('$top', String(Number(dom.topInput.value) || UI_CONFIG.DEFAULT_TOP));
  params.set('$skip', String(Number(dom.skipInput.value) || UI_CONFIG.DEFAULT_SKIP));

  if (dom.countInput.checked) {
    params.set('$count', 'true');
  }

  return params;
};

const renderTable = (rows) => {
  const head = dom.table.querySelector('thead');
  const body = dom.table.querySelector('tbody');
  head.innerHTML = '';
  body.innerHTML = '';

  if (!rows.length) {
    dom.resultSummary.textContent = 'Consulta sin filas.';
    return;
  }

  const columns = Object.keys(rows[0]);
  const trHead = document.createElement('tr');
  columns.forEach((column) => {
    const th = document.createElement('th');
    th.textContent = column;
    trHead.appendChild(th);
  });
  head.appendChild(trHead);

  rows.forEach((row) => {
    const tr = document.createElement('tr');
    columns.forEach((column) => {
      const td = document.createElement('td');
      const value = row[column];
      td.textContent = typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? '');
      tr.appendChild(td);
    });
    body.appendChild(tr);
  });

  dom.resultSummary.textContent = `Filas recibidas: ${rows.length}`;
};

const fetchRecipes = async () => {
  const query = buildQuery();
  const url = `${UI_CONFIG.API_PROXY_URL}?${query.toString()}`;
  dom.generatedUrl.textContent = url;
  dom.requestInfo.textContent = `GET ${url}`;

  setStatus('loading', 'Loading');
  try {
    const response = await fetch(url, { headers: UI_CONFIG.REQUEST_HEADERS });
    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload?.message || 'Request failed');
    }

    const rows = Array.isArray(payload?.value) ? payload.value : [];
    renderTable(rows);
    dom.rawJson.textContent = JSON.stringify(payload, null, 2);
    setStatus('success', 'Success');
  } catch (error) {
    renderTable([]);
    dom.rawJson.textContent = JSON.stringify({ error: error.message }, null, 2);
    dom.resultSummary.textContent = 'Error en consulta';
    setStatus('error', 'Error');
  }
};

const createFilterField = (definition) => {
  const label = document.createElement('label');
  label.htmlFor = definition.id;
  label.textContent = definition.label;

  const input = document.createElement('input');
  input.id = definition.id;
  input.placeholder = definition.placeholder;
  input.type = definition.type === 'number' ? 'number' : 'text';

  label.appendChild(input);
  state.filterInputs.set(definition.id, input);
  return label;
};

const mountFilters = () => {
  FILTER_DEFINITIONS.forEach((definition) => {
    dom.filterGrid.appendChild(createFilterField(definition));
  });
};

const mountOrdering = () => {
  ORDER_FIELDS.forEach((field) => {
    const option = document.createElement('option');
    option.value = field;
    option.textContent = field;
    dom.orderField.appendChild(option);
  });

  UI_CONFIG.ORDER_DIRECTIONS.forEach((direction) => {
    const option = document.createElement('option');
    option.value = direction;
    option.textContent = direction;
    dom.orderDirection.appendChild(option);
  });

  dom.orderField.value = 'creationDate';
  dom.orderDirection.value = 'desc';
};

const clearFilters = () => {
  state.filterInputs.forEach((input) => {
    input.value = '';
  });
  dom.orderField.value = 'creationDate';
  dom.orderDirection.value = 'desc';
  dom.topInput.value = String(UI_CONFIG.DEFAULT_TOP);
  dom.skipInput.value = String(UI_CONFIG.DEFAULT_SKIP);
  dom.countInput.checked = UI_CONFIG.DEFAULT_COUNT;
  dom.generatedUrl.textContent = `${UI_CONFIG.API_PROXY_URL}?$top=${UI_CONFIG.DEFAULT_TOP}`;
  dom.rawJson.textContent = '';
  dom.resultSummary.textContent = 'Sin resultados todavía.';
  setStatus('idle', 'Idle');
};

const wireEvents = () => {
  dom.searchBtn.addEventListener('click', fetchRecipes);
  dom.clearBtn.addEventListener('click', clearFilters);
};

const bootstrap = () => {
  mountFilters();
  mountOrdering();
  wireEvents();
  clearFilters();
};

bootstrap();

export const UI_CONFIG = Object.freeze({
  APP_NAME: 'Recipes OData Explorer',
  API_PROXY_URL: '/api/recipes',
  DEFAULT_TOP: 5,
  DEFAULT_SKIP: 0,
  DEFAULT_COUNT: true,
  ORDER_DIRECTIONS: ['asc', 'desc'],
  REQUEST_HEADERS: {
    Accept: 'application/json;odata.metadata=minimal'
  }
});

export const FILTER_DEFINITIONS = Object.freeze([
  { id: 'nameEq', label: 'Name =', field: 'name', operator: 'eq', type: 'text', placeholder: 'RECETA_1' },
  { id: 'nameContains', label: 'Name contains', field: 'name', operator: 'contains', type: 'text', placeholder: 'REC' },
  { id: 'nameStartsWith', label: 'Name starts with', field: 'name', operator: 'startswith', type: 'text', placeholder: 'REC' },
  { id: 'statusEq', label: 'Status =', field: 'status', operator: 'eq', type: 'text', placeholder: 'Released' },
  { id: 'revisionEq', label: 'Revision =', field: 'revision', operator: 'eq', type: 'text', placeholder: '1' },
  { id: 'creationDateGe', label: 'CreationDate >=', field: 'creationDate', operator: 'ge', type: 'datetime', placeholder: '2025-01-01T00:00:00Z' },
  { id: 'effectivityStartTimeGe', label: 'EffectivityStart >=', field: 'effectivityStartTime', operator: 'ge', type: 'datetime', placeholder: '2025-01-01T00:00:00Z' },
  { id: 'effectivityEndTimeLe', label: 'EffectivityEnd <=', field: 'effectivityEndTime', operator: 'le', type: 'datetime', placeholder: '2026-01-01T00:00:00Z' },
  { id: 'accessPrivilegeEq', label: 'AccessPrivilege =', field: 'accessPrivilege', operator: 'eq', type: 'text', placeholder: 'DEFAULT' },
  { id: 'materialDescriptionEq', label: 'MaterialDescription =', field: 'materialDescription', operator: 'eq', type: 'text', placeholder: 'MATERIAL_X' },
  { id: 'quantityValueGe', label: 'Quantity/value >=', field: 'quantity/value', operator: 'ge', type: 'number', placeholder: '10' },
  { id: 'procedureEq', label: 'Procedure =', field: 'procedure', operator: 'eq', type: 'text', placeholder: 'PROC_1' }
]);

export const ORDER_FIELDS = Object.freeze([
  'name',
  'description',
  'revision',
  'creationDate',
  'effectivityStartTime',
  'effectivityEndTime',
  'accessPrivilege'
]);

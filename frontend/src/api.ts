export type CopyStatus = 'AVAILABLE' | 'ON_LOAN' | 'LOST' | 'DAMAGED'

export interface Page<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

export interface Book {
    id: number;
    title: string;
    author: string;
    isbn: string | null;
    publisher: string | null
    publicationYear: number | null;
    category: string | null;
    description: string | null;
    active: boolean
    createdAt: string;
    updatedAt: string
}

export type BookInput = Pick<Book, 'title' | 'author' | 'isbn' | 'publisher' | 'publicationYear' | 'category' | 'description'>

export interface Copy {
    id: number;
    bookId: number;
    bookTitle: string;
    inventoryCode: string;
    status: CopyStatus
    createdAt: string;
    updatedAt: string
}

export interface Borrower {
    id: number;
    firstName: string;
    lastName: string;
    email: string | null;
    phone: string | null;
    notes: string | null
    active: boolean;
    createdAt: string;
    updatedAt: string
}

export type BorrowerInput = Pick<Borrower, 'firstName' | 'lastName' | 'email' | 'phone' | 'notes'>

export interface Loan {
    id: number;
    copyId: number;
    inventoryCode: string;
    bookId: number;
    bookTitle: string;
    borrowerId: number
    borrowerFirstName: string;
    borrowerLastName: string;
    loanDate: string;
    dueDate: string;
    returnDate: string | null
    active: boolean;
    overdue: boolean;
    notes: string | null;
    createdAt: string;
    updatedAt: string
}

export interface Dashboard {
    totalBookCount: number;
    activeBookCount: number;
    totalCopyCount: number;
    availableCopyCount: number
    onLoanCopyCount: number;
    activeLoanCount: number;
    overdueLoanCount: number;
    recentLoans: Loan[];
    overdueLoans: Loan[]
}

export interface ImportResult {
    totalRows: number;
    importedRows: number;
    failedRows: number;
    errors: { rowNumber: number; message: string }[]
}

export interface ApiProblem {
    status?: number;
    message?: string;
    fieldErrors?: Record<string, string>
}

const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`, init)
    if (!response.ok) {
        let problem: ApiProblem = {message: `İstek başarısız (${response.status}).`}
        try {
            problem = {...problem, ...await response.json() as ApiProblem}
        } catch { /* response may be empty */
        }
        throw problem
    }
    if (response.status === 204) return undefined as T
    return response.json() as Promise<T>
}

function query(params: Record<string, string | number | boolean | undefined>) {
    const search = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== '') search.set(key, String(value))
    })
    return search.size ? `?${search.toString()}` : ''
}

const json = (body: unknown): RequestInit => ({
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(body)
})
const put = (body: unknown): RequestInit => ({...json(body), method: 'PUT'})
const patch = (body: unknown): RequestInit => ({...json(body), method: 'PATCH'})

export const api = {
    dashboard: () => request<Dashboard>('/dashboard'),
    books: (params: Record<string, string | number | boolean | undefined> = {}) => request<Page<Book>>(`/books${query({
        size: 20,
        sort: 'title,asc', ...params
    })}`),
    book: (id: number) => request<Book>(`/books/${id}`),
    createBook: (body: BookInput) => request<Book>('/books', json(body)),
    updateBook: (id: number, body: BookInput) => request<Book>(`/books/${id}`, put(body)),
    archiveBook: (id: number) => request<void>(`/books/${id}`, {method: 'DELETE'}),
    copies: (bookId: number, params: Record<string, string | number | undefined> = {}) => request<Page<Copy>>(`/books/${bookId}/copies${query({size: 20, ...params})}`),
    createCopy: (bookId: number, inventoryCode: string) => request<Copy>(`/books/${bookId}/copies`, json({inventoryCode})),
    updateCopyStatus: (id: number, status: CopyStatus) => request<Copy>(`/copies/${id}/status`, patch({status})),
    borrowers: (params: Record<string, string | number | boolean | undefined> = {}) => request<Page<Borrower>>(`/borrowers${query({
        size: 20,
        sort: 'lastName,asc', ...params
    })}`),
    createBorrower: (body: BorrowerInput) => request<Borrower>('/borrowers', json(body)),
    updateBorrower: (id: number, body: BorrowerInput) => request<Borrower>(`/borrowers/${id}`, put(body)),
    borrowerStatus: (id: number, active: boolean) => request<Borrower>(`/borrowers/${id}/status`, patch({active})),
    loans: (params: Record<string, string | number | boolean | undefined> = {}) => request<Page<Loan>>(`/loans${query({
        size: 20,
        sort: 'loanDate,desc', ...params
    })}`),
    createLoan: (body: {
        copyId: number;
        borrowerId: number;
        loanDate: string;
        dueDate: string;
        notes: string | null
    }) => request<Loan>('/loans', json(body)),
    returnLoan: (id: number, returnDate: string, copyStatus?: CopyStatus) => request<Loan>(`/loans/${id}/return`, patch({
        returnDate,
        copyStatus: copyStatus || null
    })),
    importBooks: async (file: File) => {
        const data = new FormData();
        data.append('file', file)
        return request<ImportResult>('/books/import', {method: 'POST', body: data})
    },
    exportUrl: (params: Record<string, string | boolean | undefined>) => `${baseUrl}/books/export${query(params)}`,
}

export function errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'message' in error && typeof error.message === 'string') return error.message
    return 'Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.'
}

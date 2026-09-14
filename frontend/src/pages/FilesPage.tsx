import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Download,
  Eye,
  File,
  FileImage,
  FileSpreadsheet,
  FileText,
  HardDrive,
  LoaderCircle,
  Paperclip,
  ShieldCheck,
  Trash2,
  UploadCloud,
} from 'lucide-react';
import { useEffect, useRef, useState, type DragEvent } from 'react';
import { filesApi } from '@/api/services';
import { invalidateWorkspaceData } from '@/app/queryClient';
import { useAuth } from '@/auth/AuthContext';
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Modal,
  PageHeader,
  PageSkeleton,
  Pagination,
} from '@/components/ui';
import { useToast } from '@/components/ui/Toast';
import { canManage } from '@/lib/constants';
import { cn, downloadBlob, formatDate, getApiMessage } from '@/lib/utils';
import type { FileDetails, FileSummary } from '@/types';

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACCEPTED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'application/pdf', 'text/csv']);
const ACCEPT = [...ACCEPTED_TYPES].join(',');

function formatBytes(bytes: number) {
  if (!Number.isFinite(bytes) || bytes < 0) return '—';
  if (bytes < 1024) return `${bytes} B`;
  const units = ['KB', 'MB', 'GB'];
  let value = bytes / 1024;
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  return `${value >= 10 ? value.toFixed(0) : value.toFixed(1)} ${units[index]}`;
}

function fileKind(file: Pick<FileSummary, 'contentType'>) {
  if (file.contentType.startsWith('image/')) return { label: 'Image', icon: FileImage, tone: 'info' as const };
  if (file.contentType === 'application/pdf') return { label: 'PDF', icon: FileText, tone: 'danger' as const };
  if (file.contentType === 'text/csv') return { label: 'CSV', icon: FileSpreadsheet, tone: 'success' as const };
  return { label: 'File', icon: File, tone: 'neutral' as const };
}

function validateUpload(file: globalThis.File) {
  if (!file.size) return 'Choose a file that is not empty.';
  if (file.size > MAX_FILE_SIZE) return 'Files must be 10 MB or smaller.';
  if (!ACCEPTED_TYPES.has(file.type)) return 'Use a JPG, PNG, WebP, PDF, or CSV file.';
  return null;
}

interface PreviewState {
  file: FileDetails;
  blob: Blob;
  objectUrl: string;
}

function FilePreview({ preview }: { preview: PreviewState }) {
  if (preview.file.contentType.startsWith('image/')) {
    return <img src={preview.objectUrl} alt={preview.file.filename} className="mx-auto max-h-[65vh] max-w-full rounded-2xl object-contain" />;
  }
  if (preview.file.contentType === 'application/pdf') {
    return <iframe src={preview.objectUrl} title={preview.file.filename} className="h-[65vh] w-full rounded-2xl border border-ink-200 bg-white dark:border-ink-700" />;
  }
  return (
    <div className="grid min-h-64 place-items-center rounded-2xl border border-dashed border-ink-300 bg-ink-50 p-8 text-center dark:border-ink-700 dark:bg-ink-950/50">
      <div><FileSpreadsheet className="mx-auto h-12 w-12 text-brand-600" /><p className="mt-4 text-sm font-semibold text-ink-900 dark:text-white">Preview is not available for this file type.</p><p className="mt-1 text-xs text-ink-500">Download the file to open it in its usual application.</p></div>
    </div>
  );
}

export function FilesPage() {
  const [page, setPage] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [openingId, setOpeningId] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [preview, setPreview] = useState<PreviewState | null>(null);
  const [deleting, setDeleting] = useState<FileSummary | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const { user } = useAuth();
  const { toast } = useToast();
  const deletable = canManage(user?.role);

  const query = useQuery({
    queryKey: ['files', page],
    queryFn: () => filesApi.list({ page, size: 20 }),
  });
  const rows = query.data?.content ?? [];
  const visibleBytes = rows.reduce((total, file) => total + file.size, 0);

  useEffect(() => () => {
    if (preview?.objectUrl) URL.revokeObjectURL(preview.objectUrl);
  }, [preview?.objectUrl]);

  const upload = useMutation({
    mutationFn: (file: globalThis.File) => filesApi.upload(file, setUploadProgress),
    onMutate: () => setUploadProgress(1),
    onSuccess: async (_stored, file) => {
      setPage(0);
      await invalidateWorkspaceData('files');
      toast('File uploaded', { description: `${file.name} is now available to your workspace.` });
    },
    onError: (error) => toast('Could not upload file', { kind: 'error', description: getApiMessage(error) }),
    onSettled: () => setUploadProgress(0),
  });

  const remove = useMutation({
    mutationFn: (file: FileSummary) => filesApi.remove(file.key),
    onSuccess: async (_response, file) => {
      if (page > 0 && rows.length === 1) setPage((current) => Math.max(0, current - 1));
      await invalidateWorkspaceData('files');
      setDeleting(null);
      toast('File deleted', { description: `${file.filename} was removed from the workspace.` });
    },
    onError: (error) => toast('Could not delete file', { kind: 'error', description: getApiMessage(error) }),
  });

  const chooseFile = (file?: globalThis.File) => {
    if (!file || upload.isPending) return;
    const message = validateUpload(file);
    if (message) {
      toast('File cannot be uploaded', { kind: 'error', description: message });
      return;
    }
    upload.mutate(file);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragging(false);
    chooseFile(event.dataTransfer.files.item(0) ?? undefined);
  };

  const loadFile = async (file: FileSummary) => {
    const details = await filesApi.get(file.id);
    const downloaded = await filesApi.content(details);
    const blob = downloaded.type === details.contentType ? downloaded : new Blob([downloaded], { type: details.contentType });
    return { details, blob };
  };

  const openFile = async (file: FileSummary) => {
    setOpeningId(file.id);
    try {
      const { details, blob } = await loadFile(file);
      setPreview({ file: details, blob, objectUrl: URL.createObjectURL(blob) });
    } catch (error) {
      toast('Could not open file', { kind: 'error', description: getApiMessage(error) });
    } finally {
      setOpeningId(null);
    }
  };

  const downloadFile = async (file: FileSummary) => {
    setDownloadingId(file.id);
    try {
      const { details, blob } = await loadFile(file);
      downloadBlob(blob, details.filename);
    } catch (error) {
      toast('Could not download file', { kind: 'error', description: getApiMessage(error) });
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Workspace library" title="Files" description="Keep sales collateral, imports, and reference material in your tenant-isolated workspace." actions={<Button onClick={() => inputRef.current?.click()} disabled={upload.isPending}><UploadCloud className="h-4 w-4" />Upload file</Button>} />

      <div className="grid gap-4 lg:grid-cols-[1.4fr_.6fr]">
        <Card
          className={cn('relative overflow-hidden border-2 border-dashed p-6 transition sm:p-8', dragging ? 'border-brand-500 bg-brand-50 dark:bg-brand-950/40' : 'enterprise-gradient border-ink-200 dark:border-ink-700 dark:bg-ink-900 dark:bg-none')}
          onDragEnter={(event) => { event.preventDefault(); setDragging(true); }}
          onDragOver={(event) => event.preventDefault()}
          onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node)) setDragging(false); }}
          onDrop={handleDrop}
          aria-busy={upload.isPending}
        >
          <input ref={inputRef} type="file" accept={ACCEPT} className="sr-only" onChange={(event) => { chooseFile(event.target.files?.[0]); event.target.value = ''; }} />
          <div className="flex flex-col items-center text-center sm:flex-row sm:text-left">
            <div className="grid h-16 w-16 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-sky-600 text-white shadow-sm">{upload.isPending ? <LoaderCircle className="h-7 w-7 animate-spin" /> : <UploadCloud className="h-7 w-7" />}</div>
            <div className="mt-4 sm:ml-5 sm:mt-0"><h2 className="font-display text-lg font-bold text-ink-950 dark:text-white">{upload.isPending ? 'Uploading securely…' : dragging ? 'Drop your file here' : 'Drop a file or browse your computer'}</h2><p className="mt-1 text-sm leading-6 text-ink-500 dark:text-ink-400">JPG, PNG, WebP, PDF, or CSV · maximum 10 MB</p>{!upload.isPending && <button type="button" onClick={() => inputRef.current?.click()} className="mt-3 text-sm font-bold text-brand-700 hover:text-brand-800 dark:text-brand-400">Choose a file</button>}</div>
          </div>
          {upload.isPending && <div className="mt-6"><div className="flex justify-between text-xs font-semibold text-ink-500"><span>Encrypted transfer in progress</span><span>{uploadProgress}%</span></div><div className="mt-2 h-2 overflow-hidden rounded-full bg-ink-100 dark:bg-ink-800"><div className="h-full rounded-full bg-gradient-to-r from-brand-600 to-sky-500 transition-[width] duration-300" style={{ width: `${uploadProgress}%` }} /></div></div>}
        </Card>

        <Card className="flex flex-col justify-between overflow-hidden p-6">
          <div className="flex items-start justify-between"><div><p className="text-xs font-bold uppercase tracking-[.16em] text-ink-400">Workspace usage</p><p className="mt-3 font-display text-3xl font-bold text-ink-950 dark:text-white">{query.data?.totalElements ?? '—'}</p><p className="mt-1 text-sm text-ink-500">stored {query.data?.totalElements === 1 ? 'file' : 'files'}</p></div><div className="grid h-11 w-11 place-items-center rounded-xl bg-indigo-100 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300"><HardDrive className="h-5 w-5" /></div></div>
          <div className="mt-5 border-t border-ink-100 pt-4 dark:border-ink-800"><p className="text-xs text-ink-500">This page: <span className="font-semibold text-ink-800 dark:text-ink-200">{formatBytes(visibleBytes)}</span></p><p className="mt-2 flex items-center gap-1.5 text-xs text-brand-700 dark:text-brand-400"><ShieldCheck className="h-3.5 w-3.5" />Tenant-isolated access</p></div>
        </Card>
      </div>

      {query.isLoading ? <PageSkeleton /> : query.isError ? <Card><ErrorState onRetry={() => void query.refetch()} title="We could not load your files" /></Card> : !rows.length ? <Card><EmptyState icon={Paperclip} title="Your file library is empty" description="Upload a document, image, or CSV to make it available to this workspace." action={<Button onClick={() => inputRef.current?.click()}><UploadCloud className="h-4 w-4" />Upload your first file</Button>} /></Card> : (
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-ink-100 px-4 py-4 dark:border-ink-800 sm:px-5"><div><h2 className="font-display font-bold text-ink-950 dark:text-white">Workspace files</h2><p className="mt-0.5 text-xs text-ink-500">Page {(query.data?.page ?? 0) + 1} · newest first</p></div><Badge tone="brand">{rows.length} visible</Badge></div>
          <div className="divide-y divide-ink-100 dark:divide-ink-800">
            {rows.map((file) => {
              const kind = fileKind(file);
              const Icon = kind.icon;
              return <article key={file.id} className="flex flex-col gap-4 px-4 py-4 transition hover:bg-ink-50 dark:hover:bg-ink-800/60 sm:flex-row sm:items-center sm:px-5"><div className="flex min-w-0 flex-1 items-center gap-3.5"><div className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300"><Icon className="h-5 w-5" /></div><div className="min-w-0"><h3 className="truncate text-sm font-semibold text-ink-950 dark:text-white" title={file.filename}>{file.filename}</h3><div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-ink-500"><Badge tone={kind.tone}>{kind.label}</Badge><span>{formatBytes(file.size)}</span><span aria-hidden="true">·</span><span>{formatDate(file.createdAt)}</span></div></div></div><div className="flex items-center gap-1 self-end sm:self-auto"><Button variant="ghost" size="sm" onClick={() => void openFile(file)} disabled={openingId === file.id} aria-label={`Preview ${file.filename}`}>{openingId === file.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Eye className="h-4 w-4" />}<span className="hidden md:inline">Preview</span></Button><Button variant="ghost" size="sm" onClick={() => void downloadFile(file)} disabled={downloadingId === file.id} aria-label={`Download ${file.filename}`}>{downloadingId === file.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}</Button>{deletable && <Button variant="ghost" size="sm" className="text-rose-600" onClick={() => setDeleting(file)} aria-label={`Delete ${file.filename}`}><Trash2 className="h-4 w-4" /></Button>}</div></article>;
            })}
          </div>
          <Pagination page={query.data?.page ?? page} totalPages={query.data?.totalPages ?? 0} onChange={setPage} />
        </Card>
      )}

      <Modal open={Boolean(preview)} onClose={() => setPreview(null)} title={preview?.file.filename ?? 'File preview'} description={preview ? `${fileKind(preview.file).label} · ${formatBytes(preview.file.size)}` : undefined} width="max-w-5xl">
        {preview && <><FilePreview preview={preview} /><div className="mt-5 flex justify-end"><Button variant="secondary" onClick={() => downloadBlob(preview.blob, preview.file.filename)}><Download className="h-4 w-4" />Download a copy</Button></div></>}
      </Modal>
      <ConfirmDialog open={Boolean(deleting)} onClose={() => setDeleting(null)} onConfirm={() => deleting && remove.mutate(deleting)} busy={remove.isPending} title="Delete this file?" description={`“${deleting?.filename ?? 'This file'}” will be permanently removed from this workspace. This action cannot be undone.`} />
    </div>
  );
}
